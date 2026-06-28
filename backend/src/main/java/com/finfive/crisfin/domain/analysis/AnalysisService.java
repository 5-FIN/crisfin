package com.finfive.crisfin.domain.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finfive.crisfin.domain.analysis.dto.AnalysisRequest;
import com.finfive.crisfin.domain.analysis.dto.AnalysisResultResponse;
import com.finfive.crisfin.domain.crisis.CrisisType;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import com.finfive.crisfin.global.filter.LlmResponseValidator;
import com.finfive.crisfin.global.filter.PiiMaskingService;
import com.finfive.crisfin.global.filter.PromptInjectionDetector;
import com.finfive.crisfin.infra.llm.LlmProviderRouter;
import com.finfive.crisfin.infra.llm.dto.LlmRequest;
import com.finfive.crisfin.infra.llm.dto.LlmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;

/**
 * Core application service for AI-powered financial crisis analysis.
 *
 * <p>Orchestrates input validation, PII masking, prompt construction, LLM invocation,
 * response validation, and persistence of the analysis result.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnalysisService {

    private final LlmProviderRouter llmProviderRouter;
    private final AnalysisResultRepository analysisResultRepository;
    private final PiiMaskingService piiMaskingService;
    private final PromptInjectionDetector promptInjectionDetector;
    private final LlmResponseValidator llmResponseValidator;
    private final SystemPromptProvider systemPromptProvider;
    private final ObjectMapper objectMapper;

    /**
     * Runs the full analysis pipeline for the given request.
     *
     * <ol>
     *   <li>Detect prompt injection in the situation description.</li>
     *   <li>Mask PII in the supplied MyData snapshot.</li>
     *   <li>Resolve the {@link CrisisType} enum from the request string.</li>
     *   <li>Build and send the LLM request.</li>
     *   <li>Validate and parse the LLM response JSON.</li>
     *   <li>Persist the result and return the response DTO.</li>
     * </ol>
     *
     * @param req    validated analysis request
     * @param userId nullable; {@code null} for anonymous callers
     * @return populated {@link AnalysisResultResponse}
     */
    @Transactional
    public AnalysisResultResponse recommend(AnalysisRequest req, Long userId) {

        // Step 1: prompt injection guard
        promptInjectionDetector.validate(req.getSituationDescription());

        // Step 2: PII masking
        Map<String, Object> rawMyData = req.getFilteredMyData();
        Map<String, Object> maskedData = (rawMyData != null)
                ? piiMaskingService.maskJsonData(rawMyData)
                : Collections.emptyMap();

        // Step 3: resolve CrisisType enum
        CrisisType crisisType = parseCrisisType(req.getCrisisType());

        // Step 4: build system prompt
        String systemPrompt = systemPromptProvider.getSystemPrompt(crisisType);

        // Step 5: build user message
        String userMessage = buildUserMessage(crisisType, req.getSituationDescription(), maskedData);

        log.info("[AnalysisService] Sending LLM request for crisisType={}, userId={}", crisisType, userId);

        // Step 6: call LLM via router (with provider fallback)
        LlmResponse llmResponse = llmProviderRouter.complete(
                LlmRequest.builder()
                        .systemPrompt(systemPrompt)
                        .userMessage(userMessage)
                        .maxTokens(2048)
                        .temperature(0.3)
                        .build()
        );

        // Step 7: validate and parse LLM JSON response
        JsonNode resultNode = llmResponseValidator.validateAndParse(llmResponse.getContent());

        // Step 8: convert JsonNode → Map for persistence
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = objectMapper.convertValue(resultNode, Map.class);

        // Step 9: persist
        AnalysisResult saved = analysisResultRepository.save(
                AnalysisResult.builder()
                        .userId(userId)
                        .crisisType(crisisType)
                        .situationDescription(req.getSituationDescription())
                        .inputMyDataJson(maskedData)
                        .resultJson(resultMap)
                        .llmProvider(llmResponse.getProviderName())
                        .tokensUsed(llmResponse.getInputTokens() + llmResponse.getOutputTokens())
                        .build()
        );

        log.info("[AnalysisService] Analysis saved with id={}, provider={}", saved.getId(), saved.getLlmProvider());

        // Step 10: build response DTO
        return toResponse(saved, resultNode);
    }

    /**
     * Retrieves a previously saved analysis result by its ID.
     *
     * @param id analysis result primary key
     * @return the corresponding response DTO
     * @throws CrisfinException with {@link ErrorCode#GUIDE_NOT_FOUND} if not found
     */
    @Transactional(readOnly = true)
    public AnalysisResultResponse getResult(Long id) {
        AnalysisResult result = analysisResultRepository.findById(id)
                .orElseThrow(() -> new CrisfinException(ErrorCode.GUIDE_NOT_FOUND,
                        "분석 결과를 찾을 수 없습니다. id=" + id));

        JsonNode resultNode = objectMapper.valueToTree(result.getResultJson());
        return toResponse(result, resultNode);
    }

    // ------------------------------------------------------------------ //
    //  Private helpers
    // ------------------------------------------------------------------ //

    private CrisisType parseCrisisType(String raw) {
        try {
            return CrisisType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CrisfinException(ErrorCode.CRISIS_TYPE_NOT_FOUND,
                    "알 수 없는 위기 유형입니다: " + raw);
        }
    }

    private String buildUserMessage(CrisisType crisisType,
                                    String situationDescription,
                                    Map<String, Object> maskedData) {
        String myDataJson;
        try {
            myDataJson = objectMapper.writeValueAsString(maskedData);
        } catch (Exception e) {
            log.warn("[AnalysisService] MyData 직렬화 실패, 빈 객체로 대체합니다: {}", e.getMessage());
            myDataJson = "{}";
        }

        return String.format(
                "위기 유형: %s (%s)\n\n상황 설명: %s\n\n재무 데이터(익명화됨): %s",
                crisisType.name(),
                crisisType.getLabel(),
                situationDescription,
                myDataJson
        );
    }

    private AnalysisResultResponse toResponse(AnalysisResult entity, JsonNode resultNode) {
        return AnalysisResultResponse.builder()
                .id(entity.getId())
                .crisisType(entity.getCrisisType().name())
                .situationDescription(entity.getSituationDescription())
                .result(resultNode)
                .llmProvider(entity.getLlmProvider())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
