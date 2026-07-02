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
            root = objectMapper.readTree(extractJson(json));
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

    /**
     * LLM이 순수 JSON만 반환하도록 지시받았더라도 실제로는 ```json 코드펜스나 앞뒤
     * 설명 문장을 덧붙이는 경우가 있다. 파싱 전에 최외곽 JSON 객체 본문만 안전하게
     * 추출해 프로바이더(GEMINI/CLAUDE/OPENAI)와 무관하게 견고성을 높인다.
     *
     * @param raw LLM 원문
     * @return 추출된 JSON 문자열(추출 불가 시 원문 trim)
     */
    private String extractJson(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();

        // ```json ... ``` 또는 ``` ... ``` 코드펜스 제거
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline >= 0) {
                s = s.substring(firstNewline + 1);
            }
            int closingFence = s.lastIndexOf("```");
            if (closingFence >= 0) {
                s = s.substring(0, closingFence);
            }
            s = s.trim();
        }

        // 앞뒤에 설명 문장이 남아 있으면 최외곽 중괄호 범위만 취한다
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        return s;
    }
}
