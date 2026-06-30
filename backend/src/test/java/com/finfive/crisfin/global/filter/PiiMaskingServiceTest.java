package com.finfive.crisfin.global.filter;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PiiMaskingService} — regex masking + recursive map/list traversal.
 */
class PiiMaskingServiceTest {

    private final PiiMaskingService service = new PiiMaskingService();

    // ── maskString: 패턴별 마스킹 ──

    @Test
    void maskString_phoneNumber_isReplaced() {
        assertThat(service.maskString("01012345678")).isEqualTo("***-****-****");
        assertThat(service.maskString("연락처 01012345678 입니다"))
                .isEqualTo("연락처 ***-****-**** 입니다");
    }

    @Test
    void maskString_residentRegistrationNumber_isReplaced() {
        assertThat(service.maskString("900101-1234567")).isEqualTo("[주민번호제거]");
    }

    @Test
    void maskString_cardNumber_keepsPrefixOnly() {
        // NNNN-NNNN-NNNN-NNNN → 앞 6자 + "-**-****"
        assertThat(service.maskString("1234-5678-9012-3456")).isEqualTo("1234-5-**-****");
    }

    @Test
    void maskString_plainText_isUnchanged() {
        assertThat(service.maskString("그냥 평범한 문장입니다")).isEqualTo("그냥 평범한 문장입니다");
    }

    @Test
    void maskString_null_returnsNull() {
        assertThat(service.maskString(null)).isNull();
    }

    // ── maskJsonData: 민감 키 + 재귀 ──

    @Test
    void maskJsonData_sensitiveKeys_areReplacedWithPlaceholder() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "홍길동");
        data.put("employer", "ACME");

        Map<String, Object> masked = service.maskJsonData(data);

        assertThat(masked.get("name")).isEqualTo("[개인정보제거]");
        assertThat(masked.get("employer")).isEqualTo("[개인정보제거]");
    }

    @Test
    void maskJsonData_traversesNestedMapsAndLists() {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("name", "홍길동");
        user.put("phone", "01012345678");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", user);
        data.put("contacts", List.of("01099998888", "메모"));

        Map<String, Object> masked = service.maskJsonData(data);

        @SuppressWarnings("unchecked")
        Map<String, Object> maskedUser = (Map<String, Object>) masked.get("user");
        assertThat(maskedUser.get("name")).isEqualTo("[개인정보제거]");
        assertThat(maskedUser.get("phone")).isEqualTo("***-****-****");

        @SuppressWarnings("unchecked")
        List<Object> contacts = (List<Object>) masked.get("contacts");
        assertThat(contacts).containsExactly("***-****-****", "메모");
    }

    @Test
    void maskJsonData_nonStringValues_arePreserved() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("age", 42);
        data.put("active", true);

        Map<String, Object> masked = service.maskJsonData(data);

        assertThat(masked.get("age")).isEqualTo(42);
        assertThat(masked.get("active")).isEqualTo(true);
    }

    @Test
    void maskJsonData_doesNotMutateSource() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "홍길동");

        service.maskJsonData(data);

        // 원본은 그대로여야 함 (깊은 복사)
        assertThat(data.get("name")).isEqualTo("홍길동");
    }
}
