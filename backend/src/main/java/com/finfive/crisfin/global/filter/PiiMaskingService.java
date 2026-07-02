package com.finfive.crisfin.global.filter;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Masks Personally Identifiable Information (PII) from JSON-structured data maps.
 *
 * <p>Sensitive keys ({@code name}, {@code employer}) are replaced with placeholder strings.
 * String values across the entire tree are scanned and sanitised using regex patterns that
 * target phone numbers, resident registration numbers, and credit-card-like sequences.</p>
 */
@Service
public class PiiMaskingService {

    /** Keys whose entire value must be replaced with a placeholder. */
    private static final Set<String> SENSITIVE_KEYS = Set.of("name", "employer");

    // Phone number: 10-14 consecutive digits
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("[0-9]{10,14}");

    // Korean resident registration number: NNNNNN-NNNNNNN
    private static final Pattern RRN_PATTERN =
            Pattern.compile("[0-9]{6}-[0-9]{7}");

    // Card-like: NNNN-NNNN-NNNN-NNNN
    private static final Pattern CARD_PATTERN =
            Pattern.compile("([0-9]{4}-){3}[0-9]{4}");

    // 계좌번호 유사: 하이픈으로 구분된 3개 숫자 그룹, 마지막 그룹 4자리 이상
    // (예: 123-456-789012, 356-05-012345). 마지막 그룹을 4자리 이상으로 제한해
    // 날짜(2024-01-15)의 오탐을 피한다.
    private static final Pattern ACCOUNT_PATTERN =
            Pattern.compile("[0-9]{2,6}-[0-9]{2,6}-[0-9]{4,7}");

    /**
     * Returns a deep copy of {@code data} with PII fields and values masked.
     *
     * @param data source map (will NOT be mutated)
     * @return a new map with PII removed
     */
    public Map<String, Object> maskJsonData(Map<String, Object> data) {
        return maskMap(data);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> maskMap(Map<String, Object> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (SENSITIVE_KEYS.contains(key)) {
                result.put(key, "[개인정보제거]");
            } else if (value instanceof String s) {
                result.put(key, maskString(s));
            } else if (value instanceof Map<?, ?> nested) {
                result.put(key, maskMap((Map<String, Object>) nested));
            } else if (value instanceof List<?> list) {
                result.put(key, maskList(list));
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Object> maskList(List<?> list) {
        List<Object> result = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item instanceof String s) {
                result.add(maskString(s));
            } else if (item instanceof Map<?, ?> nested) {
                result.add(maskMap((Map<String, Object>) nested));
            } else if (item instanceof List<?> nested) {
                result.add(maskList(nested));
            } else {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Applies regex-based masking to a single string value.
     *
     * <ol>
     *   <li>Phone/account numbers (10-14 digits) → {@code "***-****-****"}</li>
     *   <li>Resident registration numbers (NNNNNN-NNNNNNN) → {@code "[주민번호제거]"}</li>
     *   <li>Card-like sequences (NNNN-NNNN-NNNN-NNNN) → first 6 chars + {@code "-**-****"}</li>
     * </ol>
     */
    public String maskString(String value) {
        if (value == null) {
            return null;
        }

        // Card pattern first (contains dashes, must be matched before raw-digit phone pattern)
        String masked = CARD_PATTERN.matcher(value).replaceAll(match -> {
            String full = match.group();
            return full.substring(0, 6) + "-**-****";
        });

        // Korean RRN
        masked = RRN_PATTERN.matcher(masked).replaceAll("[주민번호제거]");

        // 계좌번호(하이픈 3그룹) — 카드/주민번호 이후, 원시 숫자 전화 패턴 이전에 처리
        masked = ACCOUNT_PATTERN.matcher(masked).replaceAll("[계좌정보제거]");

        // Phone / long digit sequences
        masked = PHONE_PATTERN.matcher(masked).replaceAll("***-****-****");

        return masked;
    }
}
