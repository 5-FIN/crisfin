package com.finfive.crisfin.domain.recommendation.harness;

import java.util.List;
import java.util.Map;

/**
 * A single hallucination check over the assembled analysis result.
 *
 * <p>Verifiers operate on the mutable result map (the LLM's free-text fields — todos,
 * actions, holdable — plus rule-engine fields already overlaid). Amount fields under
 * {@code receivable}/{@code summary} are authoritative (rule engine) and are not policed
 * here; verifiers focus on the free-text surface the LLM still controls.</p>
 */
public interface OutputVerifier {

    /**
     * Inspects {@code resultMap} for violations.
     *
     * @param resultMap the assembled result (mutated in place when {@code sanitize} is true)
     * @param ctx       read-only analysis context
     * @param sanitize  when {@code true}, redact/remove {@link Severity#HARD} violations in
     *                  place and return flags marked {@code STRIPPED}; when {@code false},
     *                  only detect and return flags marked {@code FLAGGED}
     * @return flags found by this verifier (empty when clean)
     */
    List<HarnessFlag> inspect(Map<String, Object> resultMap, HarnessContext ctx, boolean sanitize);
}
