package com.finfive.crisfin.global.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Validates and parses raw JSON strings returned by an LLM provider.
 *
 * <p>Ensures the response is syntactically valid JSON and contains all fields
 * required by the analysis output schema. Logs a warning when the total
 * receivable estimate seems unrealistically high.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LlmResponseValidator {

    private static final List<String> REQUIRED_FIELDS =
            List.of("todos", "receivable", "holdable", "actions", "summary");

    private static final long SANITY_THRESHOLD = 100_000_000L;   // 1억 원

    private final ObjectMapper objectMapper;

    /**
     * Parses {@code json} into a {@link JsonNode} and validates the schema.
     *
     * @param json raw JSON string from the LLM
     * @return validated root {@link JsonNode}
     * @throws CrisfinException with {@link ErrorCode#LLM_RESPONSE_PARSE_ERROR}
     *         if the string is not valid JSON or any required field is absent
     */
    public JsonNode validateAndParse(String json) {
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("[LlmResponseValidator] JSON 파싱 실패: {}", e.getMessage());
            throw new CrisfinException(ErrorCode.LLM_RESPONSE_PARSE_ERROR,
                    "LLM 응답이 유효한 JSON 형식이 아닙니다: " + e.getOriginalMessage());
        }

        for (String field : REQUIRED_FIELDS) {
            if (!root.has(field) || root.get(field).isNull()) {
                log.error("[LlmResponseValidator] 필수 필드 누락: '{}'", field);
                throw new CrisfinException(ErrorCode.LLM_RESPONSE_PARSE_ERROR,
                        "LLM 응답에 필수 필드가 없습니다: " + field);
            }
        }

        JsonNode summary = root.path("summary");
        long totalReceivableMax = summary.path("totalReceivableMax").asLong(0L);
        if (totalReceivableMax > SANITY_THRESHOLD) {
            log.warn("[LlmResponseValidator] totalReceivableMax={}이 임계값({})을 초과합니다. " +
                            "LLM 응답을 검토하세요.",
                    totalReceivableMax, SANITY_THRESHOLD);
        }

        return root;
    }
}
