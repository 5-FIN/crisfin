package com.finfive.crisfin.domain.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finfive.crisfin.domain.analysis.dto.AnalysisRequest;
import com.finfive.crisfin.domain.analysis.dto.AnalysisResultResponse;
import com.finfive.crisfin.domain.analysis.dto.ApplicantProfile;
import com.finfive.crisfin.domain.analysis.dto.ReinferRequest;
import com.finfive.crisfin.domain.crisis.CrisisType;
import com.finfive.crisfin.domain.payment.PaymentService;
import com.finfive.crisfin.domain.recommendation.FallbackAnalysisProvider;
import com.finfive.crisfin.domain.recommendation.ResultAssembler;
import com.finfive.crisfin.domain.recommendation.harness.AnalysisHarness;
import com.finfive.crisfin.domain.recommendation.harness.HarnessContext;
import com.finfive.crisfin.domain.recommendation.harness.HarnessFlag;
import com.finfive.crisfin.domain.recommendation.harness.Severity;
import com.finfive.crisfin.domain.recommendation.rag.PolicyRetrievalService;
import com.finfive.crisfin.domain.recommendation.rag.RetrievedPolicy;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import com.finfive.crisfin.global.filter.LlmResponseValidator;
import com.finfive.crisfin.global.filter.PiiMaskingService;
import com.finfive.crisfin.global.filter.PromptInjectionDetector;
import com.finfive.crisfin.infra.llm.LlmProviderRouter;
import com.finfive.crisfin.infra.llm.dto.LlmRequest;
import com.finfive.crisfin.infra.llm.dto.LlmResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
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
    private final PolicyRetrievalService policyRetrievalService;
    private final ResultAssembler resultAssembler;
    private final AnalysisHarness analysisHarness;
    private final FallbackAnalysisProvider fallbackAnalysisProvider;
    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    /** llmProvider marker for a rule-based fallback result (no LLM was used → no paid use consumed). */
    private static final String FALLBACK_PROVIDER = "FALLBACK";

    /** Number of policy chunks to retrieve for RAG grounding. */
    @Value("${embedding.rag.top-k:5}")
    private int ragTopK;

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
        Map<String, Object> rawMyData = req.getFilteredMyData();
        Map<String, Object> maskedData = (rawMyData != null)
                ? piiMaskingService.maskJsonData(rawMyData)
                : Collections.emptyMap();
        CrisisType crisisType = parseCrisisType(req.getCrisisType());
        AnalysisResultResponse response = runPipeline(crisisType, req.getSituationDescription(), maskedData,
                req.getApplicantProfile(), userId);
        // 성공한 분석만 이용권 1회 소모. 동일 트랜잭션이라 소모가 실패하면 분석 저장도 함께 롤백된다.
        // 규칙 기반 폴백(LLM 미가용)은 유료 이용권을 소모하지 않는다.
        if (!FALLBACK_PROVIDER.equals(response.getLlmProvider())) {
            paymentService.consumeUse(userId);
        }
        return response;
    }

    /**
     * Re-runs a personalised analysis for an existing result. Any field omitted from the
     * {@link ReinferRequest} is inherited from the parent analysis.
     *
     * @param parentId the analysis to re-infer from
     * @param req      overriding inputs (all optional)
     * @param userId   the authenticated user (must own the parent analysis)
     */
    @Transactional
    public AnalysisResultResponse reinfer(Long parentId, ReinferRequest req, Long userId) {
        AnalysisResult parent = analysisResultRepository.findById(parentId)
                .orElseThrow(() -> new CrisfinException(ErrorCode.ANALYSIS_NOT_FOUND,
                        "분석 결과를 찾을 수 없습니다. id=" + parentId));

        if (parent.getUserId() != null && !parent.getUserId().equals(userId)) {
            throw new CrisfinException(ErrorCode.ANALYSIS_NOT_FOUND,
                    "분석 결과를 찾을 수 없습니다. id=" + parentId);
        }

        // Inherit from the parent where the request omits a field.
        String situation = (req.getSituationDescription() != null && !req.getSituationDescription().isBlank())
                ? req.getSituationDescription()
                : parent.getSituationDescription();

        ApplicantProfile profile = (req.getApplicantProfile() != null)
                ? req.getApplicantProfile()
                : fromProfileMap(parent.getApplicantProfileJson());

        Map<String, Object> maskedData;
        if (req.getFilteredMyData() != null) {
            maskedData = piiMaskingService.maskJsonData(req.getFilteredMyData());
        } else {
            maskedData = (parent.getInputMyDataJson() != null)
                    ? parent.getInputMyDataJson()
                    : Collections.emptyMap();
        }

        AnalysisResultResponse response = runPipeline(parent.getCrisisType(), situation, maskedData, profile, userId);
        // 성공한 재분석만 이용권 1회 소모(동일 트랜잭션). 규칙 기반 폴백은 소모하지 않는다.
        if (!FALLBACK_PROVIDER.equals(response.getLlmProvider())) {
            paymentService.consumeUse(userId);
        }
        return response;
    }

    /**
     * Shared analysis pipeline: validate → LLM strategy → rule-engine amounts + timeline →
     * persist → respond. Amounts come exclusively from the rule engine; the LLM supplies
     * strategy text only.
     */
    private AnalysisResultResponse runPipeline(CrisisType crisisType,
                                               String situationDescription,
                                               Map<String, Object> maskedData,
                                               ApplicantProfile applicantProfile,
                                               Long userId) {
        promptInjectionDetector.validate(situationDescription);

        // RAG 근거를 한 번만 검색해 ① 프롬프트 그라운딩 ② 응답 citations(근거 노출)
        // ③ judge의 근거 대조에 공유한다.
        List<RetrievedPolicy> policies =
                policyRetrievalService.retrieve(crisisType, situationDescription, ragTopK);
        String ragBlock = formatRagBlock(policies);
        List<Map<String, Object>> citations = buildCitations(policies);

        String systemPrompt = systemPromptProvider.getSystemPrompt(crisisType);
        String baseUserMessage =
                buildUserMessage(crisisType, situationDescription, maskedData, applicantProfile, ragBlock);

        log.info("[AnalysisService] Sending LLM request for crisisType={}, userId={}, ragChunks={}",
                crisisType, userId, policies.size());

        // 할루시네이션 하네스: 생성 → 검증 → (HARD 위반 시) 위반 피드백을 붙여 1회 재생성.
        // LLM 전면 실패 시에는 규칙 기반 폴백 안내로 graceful degrade 한다(에러 대신).
        Map<String, Object> resultMap = null;
        List<HarnessFlag> flags;
        String provider;
        int tokens;

        try {
            LlmResponse llmResponse = null;
            String userMessage = baseUserMessage;
            List<HarnessFlag> detectFlags = List.of();

            for (int attempt = 0; attempt <= 1; attempt++) {
                llmResponse = llmProviderRouter.complete(
                        LlmRequest.builder()
                                .systemPrompt(systemPrompt)
                                .userMessage(userMessage)
                                .maxTokens(2048)
                                .temperature(0.3)
                                .build()
                );

                JsonNode llmNode = llmResponseValidator.validateAndParse(llmResponse.getContent());
                resultMap = objectMapper.convertValue(llmNode, new TypeReference<Map<String, Object>>() {});

                // Rule engine owns receivable amounts / status / needsMoreInput / summary totals;
                // timeline is built (urgency-sorted) from the LLM strategy. LLM amounts are discarded.
                resultAssembler.enrich(resultMap, llmNode, crisisType, applicantProfile);

                HarnessContext ctx = new HarnessContext(crisisType, needsMoreInputNames(resultMap), ragBlock);
                detectFlags = analysisHarness.detect(resultMap, ctx);

                if (!AnalysisHarness.hasHard(detectFlags) || attempt == 1) {
                    break;
                }
                log.warn("[AnalysisService] 하네스 HARD 위반 감지 — 자가수정 재생성 시도. userId={}, 위반={}건",
                        userId, detectFlags.stream().filter(f -> f.severity() == Severity.HARD).count());
                userMessage = baseUserMessage + analysisHarness.buildRepairFeedback(detectFlags);
            }

            // 최종 정제: 재생성 후에도 남은 HARD 위반을 제거하고, 정제된 본문으로 타임라인 재생성.
            // 판사 등 detect 전용(비변형) 검증 결과가 유실되지 않도록 detect 플래그와 병합한다.
            HarnessContext finalCtx = new HarnessContext(crisisType, needsMoreInputNames(resultMap), ragBlock);
            List<HarnessFlag> sanitizedFlags = analysisHarness.sanitize(resultMap, finalCtx);
            flags = analysisHarness.finalizeFlags(detectFlags, sanitizedFlags);
            resultAssembler.rebuildTimeline(resultMap);
            provider = llmResponse.getProviderName();
            tokens = llmResponse.getInputTokens() + llmResponse.getOutputTokens();

        } catch (CrisfinException e) {
            if (e.getErrorCode() != ErrorCode.LLM_ALL_PROVIDERS_FAILED) {
                throw e;
            }
            // 모든 LLM 프로바이더 실패 → 규칙 기반 체크리스트 + 기관 안내로 degrade(명세 slide 7 Fallback).
            log.warn("[AnalysisService] 모든 LLM 프로바이더 실패 — 규칙 기반 폴백 안내 제공. userId={}, crisisType={}",
                    userId, crisisType);
            resultMap = fallbackAnalysisProvider.build(crisisType);
            resultAssembler.enrich(resultMap, objectMapper.valueToTree(resultMap), crisisType, applicantProfile);
            flags = List.of(new HarnessFlag("output", "LLM_UNAVAILABLE", Severity.SOFT,
                    "AI 분석 서버가 일시적으로 불가하여 규칙 기반 기본 안내를 제공합니다. 세부 자격·금액은 각 기관에 직접 확인하세요.",
                    "FLAGGED"));
            provider = FALLBACK_PROVIDER;
            tokens = 0;
        }

        resultMap.put("harnessFlags", objectMapper.convertValue(flags, List.class));
        resultMap.put("citations", citations);

        AnalysisResult saved = analysisResultRepository.save(
                AnalysisResult.builder()
                        .userId(userId)
                        .crisisType(crisisType)
                        .situationDescription(situationDescription)
                        .inputMyDataJson(maskedData)
                        .applicantProfileJson(toProfileMap(applicantProfile))
                        .resultJson(resultMap)
                        .llmProvider(provider)
                        .tokensUsed(tokens)
                        .build()
        );

        log.info("[AnalysisService] Analysis saved with id={}, provider={}", saved.getId(), saved.getLlmProvider());

        return toResponse(saved, objectMapper.valueToTree(resultMap));
    }

    /**
     * Extracts the rule-engine "추가 입력 필요" benefit names from the assembled result, for the
     * harness consistency check (added in a later phase). Safe against shape variations.
     */
    @SuppressWarnings("unchecked")
    private java.util.Set<String> needsMoreInputNames(Map<String, Object> resultMap) {
        Object nmi = resultMap == null ? null : resultMap.get("needsMoreInput");
        if (!(nmi instanceof List<?> list)) {
            return java.util.Set.of();
        }
        java.util.Set<String> names = new java.util.HashSet<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                Object name = m.get("benefitName");
                if (name instanceof String s && !s.isBlank()) {
                    names.add(s);
                }
            }
        }
        return names;
    }

    private Map<String, Object> toProfileMap(ApplicantProfile profile) {
        if (profile == null) {
            return null;
        }
        return objectMapper.convertValue(profile, new TypeReference<Map<String, Object>>() {});
    }

    private ApplicantProfile fromProfileMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        return objectMapper.convertValue(map, ApplicantProfile.class);
    }

    /**
     * Returns paginated analysis history for the given user.
     */
    @Transactional(readOnly = true)
    public Page<AnalysisResultResponse> history(Long userId, Pageable pageable) {
        return analysisResultRepository.findByUserId(userId, pageable)
                .map(r -> toResponse(r, objectMapper.valueToTree(r.getResultJson())));
    }

    /**
     * Retrieves a previously saved analysis result by its ID, enforcing ownership.
     *
     * <p>Only the user who owns the result may read it. A non-existent id and a result
     * owned by another user (or an anonymous result with no owner) both raise
     * {@link ErrorCode#ANALYSIS_NOT_FOUND} — the same response, so callers cannot probe
     * which ids exist (prevents IDOR / enumeration).</p>
     *
     * @param id     analysis result primary key
     * @param userId the authenticated caller's id (must match the result's owner)
     * @return the corresponding response DTO
     * @throws CrisfinException with {@link ErrorCode#ANALYSIS_NOT_FOUND} if missing or not owned
     */
    @Transactional(readOnly = true)
    public AnalysisResultResponse getResult(Long id, Long userId) {
        AnalysisResult result = analysisResultRepository.findById(id)
                .filter(r -> r.getUserId() != null && r.getUserId().equals(userId))
                .orElseThrow(() -> new CrisfinException(ErrorCode.ANALYSIS_NOT_FOUND,
                        "분석 결과를 찾을 수 없습니다. id=" + id));

        JsonNode resultNode = objectMapper.valueToTree(result.getResultJson());
        return toResponse(result, resultNode);
    }

    /**
     * Creates (or returns the existing) public share token for an owned analysis result.
     *
     * <p>Ownership is enforced identically to {@link #getResult(Long, Long)}: a missing id
     * and a result owned by someone else both raise {@link ErrorCode#ANALYSIS_NOT_FOUND}.
     * Idempotent — an already-shared result returns its existing token.</p>
     *
     * @param id     analysis result primary key
     * @param userId the authenticated caller's id (must own the result)
     * @return the existing or newly generated unguessable share token
     */
    @Transactional
    public String createShareLink(Long id, Long userId) {
        AnalysisResult result = analysisResultRepository.findById(id)
                .filter(r -> r.getUserId() != null && r.getUserId().equals(userId))
                .orElseThrow(() -> new CrisfinException(ErrorCode.ANALYSIS_NOT_FOUND,
                        "분석 결과를 찾을 수 없습니다. id=" + id));

        if (result.getShareToken() == null) {
            result.assignShareToken(java.util.UUID.randomUUID().toString().replace("-", ""));
        }
        return result.getShareToken();
    }

    /**
     * Revokes the public share link for an owned analysis result. Ownership is enforced
     * identically to {@link #getResult(Long, Long)}.
     *
     * @param id     analysis result primary key
     * @param userId the authenticated caller's id (must own the result)
     */
    @Transactional
    public void revokeShareLink(Long id, Long userId) {
        AnalysisResult result = analysisResultRepository.findById(id)
                .filter(r -> r.getUserId() != null && r.getUserId().equals(userId))
                .orElseThrow(() -> new CrisfinException(ErrorCode.ANALYSIS_NOT_FOUND,
                        "분석 결과를 찾을 수 없습니다. id=" + id));

        result.revokeShare();
    }

    /**
     * Retrieves a shared analysis result by its public token — no login, no ownership.
     *
     * <p>Access is granted solely by holding the unguessable token; this is the intended
     * public read-only path (distinct from the ownership-checked {@link #getResult(Long, Long)}).</p>
     *
     * @param token the public share token
     * @return the corresponding response DTO
     * @throws CrisfinException with {@link ErrorCode#ANALYSIS_NOT_FOUND} if the token is unknown
     */
    @Transactional(readOnly = true)
    public AnalysisResultResponse getSharedResult(String token) {
        AnalysisResult result = analysisResultRepository.findByShareToken(token)
                .orElseThrow(() -> new CrisfinException(ErrorCode.ANALYSIS_NOT_FOUND,
                        "공유된 분석 결과를 찾을 수 없습니다."));

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
                                    Map<String, Object> maskedData,
                                    ApplicantProfile applicantProfile,
                                    String ragBlock) {
        String myDataJson;
        try {
            myDataJson = objectMapper.writeValueAsString(maskedData);
        } catch (Exception e) {
            log.warn("[AnalysisService] MyData 직렬화 실패, 빈 객체로 대체합니다: {}", e.getMessage());
            myDataJson = "{}";
        }

        StringBuilder sb = new StringBuilder(String.format(
                "위기 유형: %s (%s)\n\n상황 설명: %s\n\n재무 데이터(익명화됨): %s",
                crisisType.name(),
                crisisType.getLabel(),
                situationDescription,
                myDataJson
        ));

        String profileLine = buildProfileLine(applicantProfile);
        if (!profileLine.isEmpty()) {
            sb.append("\n\n신청자 프로필(자격 참고용): ").append(profileLine);
        }

        // Hybrid RAG: append retrieved policy context as grounding only. When RAG is
        // disabled or finds nothing, the block is empty and the message is unchanged.
        if (ragBlock != null && !ragBlock.isEmpty()) {
            sb.append("\n\n").append(ragBlock);
        }
        return sb.toString();
    }

    /** Compact, human-readable applicant profile summary for the LLM (amounts stay in the rule engine). */
    private String buildProfileLine(ApplicantProfile p) {
        if (p == null) {
            return "";
        }
        List<String> parts = new java.util.ArrayList<>();
        if (p.getHouseholdSize() != null) parts.add("가구원 " + p.getHouseholdSize() + "인");
        if (p.getMonthlyIncome() != null) parts.add(String.format("월소득 %,d원", p.getMonthlyIncome()));
        if (p.getAge() != null) parts.add("나이 " + p.getAge());
        if (p.getEmploymentInsuranceMonths() != null) parts.add("고용보험 " + p.getEmploymentInsuranceMonths() + "개월");
        if (p.getInvoluntarySeparation() != null) parts.add(p.getInvoluntarySeparation() ? "비자발적 이직" : "자발적 이직");
        if (p.getCareGrade() != null) parts.add("장기요양 " + p.getCareGrade() + "등급");
        return String.join(", ", parts);
    }

    /**
     * Builds the RAG grounding block from the policy vector store. The retrieved policies
     * are provided strictly as evidence — the model must NOT generate amounts from them
     * (amount estimation stays in the rule engine).
     *
     * @return the formatted block, or an empty string when there is nothing to add
     */
    private String formatRagBlock(List<RetrievedPolicy> policies) {
        if (policies.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[관련 정책 발췌(RAG) — 근거로만 사용, 금액 생성 금지]\n");
        int idx = 1;
        for (RetrievedPolicy p : policies) {
            sb.append(idx++).append(". (").append(p.sourceType()).append(") ")
              .append(p.content().replaceAll("\\s+", " ").trim()).append('\n');
        }
        return sb.toString().trim();
    }

    /**
     * Converts the retrieved RAG policies into user-facing citations attached to the result,
     * so the UI can show the official sources this analysis was grounded in (근거 기반 투명성).
     */
    private List<Map<String, Object>> buildCitations(List<RetrievedPolicy> policies) {
        List<Map<String, Object>> citations = new java.util.ArrayList<>();
        for (RetrievedPolicy p : policies) {
            String snippet = p.content().replaceAll("\\s+", " ").trim();
            if (snippet.length() > 120) {
                snippet = snippet.substring(0, 120) + "…";
            }
            Map<String, Object> c = new java.util.LinkedHashMap<>();
            c.put("sourceType", p.sourceType());   // WELFARE | GUIDE
            c.put("sourceRef", p.sourceRef());      // row id within the source
            c.put("snippet", snippet);
            citations.add(c);
        }
        return citations;
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
