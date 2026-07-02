package com.finfive.crisfin.domain.recommendation.harness;

/**
 * A single hallucination-guard finding raised by the analysis harness.
 *
 * <p>Financial advice is money-critical, so the harness verifies the LLM's free-text
 * output (todos/actions/holdable) after generation. Each violation is recorded as a
 * {@code HarnessFlag} describing what was found, where, how serious it is, and what the
 * harness did about it. Flags are surfaced to the client so the UI can warn the user.</p>
 *
 * @param field    dotted path of the offending field (e.g. {@code actions[2].contactInfo})
 * @param type     machine-readable violation type (e.g. {@code FABRICATED_CONTACT})
 * @param severity {@link Severity#HARD} triggers repair/strip; {@link Severity#SOFT} is flag-only
 * @param message  human-readable Korean explanation shown to the user
 * @param action   what the harness did: {@code FLAGGED}, {@code STRIPPED}, or {@code RETRIED}
 */
public record HarnessFlag(
        String field,
        String type,
        Severity severity,
        String message,
        String action
) {
    public static HarnessFlag hard(String field, String type, String message) {
        return new HarnessFlag(field, type, Severity.HARD, message, "FLAGGED");
    }

    public static HarnessFlag soft(String field, String type, String message) {
        return new HarnessFlag(field, type, Severity.SOFT, message, "FLAGGED");
    }

    /** Returns a copy with the recorded action changed (e.g. after stripping/retrying). */
    public HarnessFlag withAction(String newAction) {
        return new HarnessFlag(field, type, severity, message, newAction);
    }
}
