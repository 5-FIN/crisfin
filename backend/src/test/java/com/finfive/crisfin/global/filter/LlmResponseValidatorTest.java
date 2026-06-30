package com.finfive.crisfin.global.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link LlmResponseValidator} — JSON 파싱 + 필수 필드 검증.
 */
class LlmResponseValidatorTest {

    private final LlmResponseValidator validator = new LlmResponseValidator(new ObjectMapper());

    private static final String VALID_JSON = """
            {
              "todos": [],
              "receivable": [],
              "holdable": [],
              "actions": [],
              "summary": { "totalReceivableMin": 0, "totalReceivableMax": 1000000 }
            }
            """;

    @Test
    void validateAndParse_validResponse_returnsRootNode() {
        JsonNode root = validator.validateAndParse(VALID_JSON);

        assertThat(root).isNotNull();
        assertThat(root.has("summary")).isTrue();
    }

    @Test
    void validateAndParse_malformedJson_throwsParseError() {
        assertThatThrownBy(() -> validator.validateAndParse("{ not valid json "))
                .isInstanceOf(CrisfinException.class)
                .extracting(e -> ((CrisfinException) e).getErrorCode())
                .isEqualTo(ErrorCode.LLM_RESPONSE_PARSE_ERROR);
    }

    @Test
    void validateAndParse_missingRequiredField_throwsParseError() {
        // "summary" 누락
        String missing = """
                { "todos": [], "receivable": [], "holdable": [], "actions": [] }
                """;

        assertThatThrownBy(() -> validator.validateAndParse(missing))
                .isInstanceOf(CrisfinException.class)
                .extracting(e -> ((CrisfinException) e).getErrorCode())
                .isEqualTo(ErrorCode.LLM_RESPONSE_PARSE_ERROR);
    }

    @Test
    void validateAndParse_nullRequiredField_throwsParseError() {
        String nullField = """
                {
                  "todos": [], "receivable": [], "holdable": [], "actions": [],
                  "summary": null
                }
                """;

        assertThatThrownBy(() -> validator.validateAndParse(nullField))
                .isInstanceOf(CrisfinException.class);
    }

    @Test
    void validateAndParse_amountOverSanityThreshold_stillReturns() {
        // 임계값(1억) 초과 시 경고 로그만 남기고 통과해야 한다.
        String highAmount = """
                {
                  "todos": [], "receivable": [], "holdable": [], "actions": [],
                  "summary": { "totalReceivableMax": 999999999 }
                }
                """;

        JsonNode root = validator.validateAndParse(highAmount);

        assertThat(root.path("summary").path("totalReceivableMax").asLong())
                .isEqualTo(999_999_999L);
    }
}
