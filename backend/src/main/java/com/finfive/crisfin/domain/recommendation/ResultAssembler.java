package com.finfive.crisfin.domain.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finfive.crisfin.domain.analysis.dto.ApplicantProfile;
import com.finfive.crisfin.domain.crisis.CrisisType;
import com.finfive.crisfin.domain.recommendation.rule.BenefitRuleEngine;
import com.finfive.crisfin.domain.recommendation.rule.RuleEvaluation;
import com.finfive.crisfin.domain.recommendation.timeline.TimelineBuilder;
import com.finfive.crisfin.domain.recommendation.timeline.TimelinePhase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Overlays deterministic rule-engine output onto the LLM strategy result, producing the
 * final result payload consumed by the front-end.
 *
 * <p>Authoritative fields written here: {@code receivable} (amounts + status + basis),
 * {@code needsMoreInput}, {@code summary.totalReceivableMin/Max}, and a 30-day urgency-sorted
 * {@code timeline}. The LLM's own receivable amounts are intentionally discarded — amount
 * generation lives only in the rule engine.</p>
 */
@Component
@RequiredArgsConstructor
public class ResultAssembler {

    private final BenefitRuleEngine benefitRuleEngine;
    private final TimelineBuilder timelineBuilder;
    private final ObjectMapper objectMapper;

    /**
     * Mutates {@code resultMap} in place, replacing/adding the rule-engine-owned fields.
     *
     * @param resultMap the LLM result as a mutable map (todos/holdable/actions/disclaimer kept)
     * @param llmNode   the original LLM result tree (source for the timeline)
     * @param crisisType the resolved crisis type
     * @param profile   applicant profile (nullable)
     */
    @SuppressWarnings("unchecked")
    public void enrich(Map<String, Object> resultMap,
                       JsonNode llmNode,
                       CrisisType crisisType,
                       ApplicantProfile profile) {
        RuleEvaluation evaluation = benefitRuleEngine.evaluate(crisisType, profile);

        resultMap.put("receivable", objectMapper.convertValue(evaluation.receivables(), List.class));
        resultMap.put("needsMoreInput", objectMapper.convertValue(evaluation.needsMoreInput(), List.class));

        List<TimelinePhase> timeline = timelineBuilder.build(llmNode);
        resultMap.put("timeline", objectMapper.convertValue(timeline, List.class));

        Object summaryObj = resultMap.get("summary");
        Map<String, Object> summary = (summaryObj instanceof Map)
                ? (Map<String, Object>) summaryObj
                : new LinkedHashMap<>();
        summary.put("totalReceivableMin", evaluation.totalReceivableMin());
        summary.put("totalReceivableMax", evaluation.totalReceivableMax());
        resultMap.put("summary", summary);
    }

    /**
     * Rebuilds the timeline from the <em>current</em> {@code resultMap} todos/actions. Called
     * after the harness sanitizes free-text fields so the timeline reflects the redacted
     * content rather than the original (pre-sanitize) LLM output.
     *
     * @param resultMap the assembled result whose timeline should be regenerated in place
     */
    public void rebuildTimeline(Map<String, Object> resultMap) {
        List<TimelinePhase> timeline = timelineBuilder.build(objectMapper.valueToTree(resultMap));
        resultMap.put("timeline", objectMapper.convertValue(timeline, List.class));
    }
}
