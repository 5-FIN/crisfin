package com.finfive.crisfin.domain.recommendation.timeline;

/**
 * A single urgency-ranked task within a {@link TimelinePhase}.
 */
public record TimelineTask(
        String title,
        String priority,
        String dayRange,
        int urgencyScore,
        String category
) {
}
