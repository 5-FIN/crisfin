package com.finfive.crisfin.domain.recommendation.rule;

import java.util.List;

/**
 * Result of a rule-engine evaluation: the receivable benefits, the benefits awaiting more
 * input, and the rule-engine total range (sum of eligible amounts only).
 */
public record RuleEvaluation(
        List<ReceivableEstimate> receivables,
        List<NeedsMoreInputItem> needsMoreInput,
        long totalReceivableMin,
        long totalReceivableMax
) {
}
