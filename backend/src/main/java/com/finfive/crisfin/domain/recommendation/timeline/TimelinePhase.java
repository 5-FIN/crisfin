package com.finfive.crisfin.domain.recommendation.timeline;

import java.util.List;

/**
 * A 30-day timeline bucket holding urgency-sorted tasks.
 *
 * @param range   display label, e.g. {@code "0-3일"}
 * @param fromDay inclusive start day
 * @param toDay   inclusive end day
 * @param items   tasks in this bucket, pre-sorted by descending urgency
 */
public record TimelinePhase(
        String range,
        int fromDay,
        int toDay,
        List<TimelineTask> items
) {
}
