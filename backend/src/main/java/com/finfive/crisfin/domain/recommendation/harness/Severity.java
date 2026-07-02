package com.finfive.crisfin.domain.recommendation.harness;

/**
 * Severity of a harness finding.
 *
 * <ul>
 *   <li>{@link #HARD} — a fabricated fact that could cause real harm (fake contact number,
 *       an amount asserted outside the rule engine, a program not in the allowed set).
 *       Triggers a self-repair retry and, if still present, stripping of the offending field.</li>
 *   <li>{@link #SOFT} — a style/quality issue (overconfident wording, absolute past dates).
 *       Flag-only; the content is kept.</li>
 * </ul>
 */
public enum Severity {
    HARD,
    SOFT
}
