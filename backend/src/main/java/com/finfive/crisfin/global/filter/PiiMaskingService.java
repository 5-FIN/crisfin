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
    String maskString(String value) {
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

        // Phone / long digit sequences
        masked = PHONE_PATTERN.matcher(masked).replaceAll("***-****-****");

        return masked;
    }
}
