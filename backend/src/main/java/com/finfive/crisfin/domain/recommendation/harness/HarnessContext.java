package com.finfive.crisfin.domain.recommendation.harness;

import com.finfive.crisfin.domain.crisis.CrisisType;

import java.util.Set;

/**
 * Read-only context passed to each {@link OutputVerifier}.
 *
 * @param crisisType      the crisis type of the current analysis
 * @param needsMoreInput  rule-engine benefit names flagged as "추가 입력 필요" (used by the
 *                        consistency check to catch the LLM asserting eligibility for them)
 */
public record HarnessContext(
        CrisisType crisisType,
        Set<String> needsMoreInput
) {
}
