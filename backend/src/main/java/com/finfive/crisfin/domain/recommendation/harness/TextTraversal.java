package com.finfive.crisfin.domain.recommendation.harness;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Walks the LLM free-text fields of an assembled result map and invokes a visitor for each,
 * giving both the value and a replacer so verifiers can redact in place.
 *
 * <p>Only fields the LLM authored are visited — {@code receivable}/{@code summary} amounts
 * are rule-engine-owned and are deliberately excluded (except {@code summary.thirtyDayPlan},
 * which is LLM narrative).</p>
 */
final class TextTraversal {

    private TextTraversal() {
    }

    /** Receives a single free-text field: its path, leaf key, value, and a value replacer. */
    interface FieldVisitor {
        void visit(String path, String leafKey, String value, Consumer<String> replacer);
    }

    /** Fields visited per array element, keyed by array name. */
    private static final Map<String, List<String>> ARRAY_FIELDS = Map.of(
            "todos", List.of("dayRange", "action", "deadline", "reason"),
            "actions", List.of("name", "deadline", "contactInfo"),
            "holdable", List.of("name", "deferPeriod", "howTo", "caution")
    );

    @SuppressWarnings("unchecked")
    static void forEachFreeText(Map<String, Object> resultMap, FieldVisitor visitor) {
        for (Map.Entry<String, List<String>> arr : ARRAY_FIELDS.entrySet()) {
            String arrayName = arr.getKey();
            Object listObj = resultMap.get(arrayName);
            if (!(listObj instanceof List<?> list)) {
                continue;
            }
            for (int i = 0; i < list.size(); i++) {
                Object itemObj = list.get(i);
                if (!(itemObj instanceof Map)) {
                    continue;
                }
                Map<String, Object> item = (Map<String, Object>) itemObj;
                for (String key : arr.getValue()) {
                    Object val = item.get(key);
                    if (val instanceof String s && !s.isBlank()) {
                        String path = arrayName + "[" + i + "]." + key;
                        visitor.visit(path, key, s, replacement -> item.put(key, replacement));
                    }
                }
            }
        }

        // summary.thirtyDayPlan (LLM narrative)
        Object summaryObj = resultMap.get("summary");
        if (summaryObj instanceof Map) {
            Map<String, Object> summary = (Map<String, Object>) summaryObj;
            Object plan = summary.get("thirtyDayPlan");
            if (plan instanceof String s && !s.isBlank()) {
                visitor.visit("summary.thirtyDayPlan", "thirtyDayPlan", s,
                        replacement -> summary.put("thirtyDayPlan", replacement));
            }
        }
    }
}
