package com.finfive.crisfin.domain.recommendation.rule;

import java.util.List;

/**
 * A single receivable benefit produced by the rule engine.
 *
 * <p>Amounts ({@code estimatedMin}/{@code estimatedMax}) are {@code null} when the engine
 * cannot compute them yet (status {@code NEEDS_MORE_INPUT}). All monetary values are KRW.</p>
 */
public record ReceivableEstimate(
        String name,
        Long estimatedMin,
        Long estimatedMax,
        String status,
        String basis,
        String source,
        String applyUrl,
        String deadline,
        List<String> requiredDocs
) {
    public static final String ELIGIBLE = "ELIGIBLE";
    public static final String NEEDS_MORE_INPUT = "NEEDS_MORE_INPUT";

    public static ReceivableEstimate eligible(String name, long min, long max, String basis,
                                              String source, String applyUrl, String deadline,
                                              List<String> requiredDocs) {
        return new ReceivableEstimate(name, min, max, ELIGIBLE, basis, source, applyUrl, deadline, requiredDocs);
    }

    public static ReceivableEstimate needsMoreInput(String name, String basis, String source,
                                                    String applyUrl, String deadline,
                                                    List<String> requiredDocs) {
        return new ReceivableEstimate(name, null, null, NEEDS_MORE_INPUT, basis, source, applyUrl, deadline, requiredDocs);
    }
}
