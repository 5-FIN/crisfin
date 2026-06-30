package com.finfive.crisfin.domain.recommendation.rule;

import java.util.List;

/**
 * A benefit that could not be evaluated because required applicant inputs are missing.
 *
 * @param benefitName   the benefit awaiting input
 * @param missingInputs human-readable names of the inputs still needed
 */
public record NeedsMoreInputItem(String benefitName, List<String> missingInputs) {
}
