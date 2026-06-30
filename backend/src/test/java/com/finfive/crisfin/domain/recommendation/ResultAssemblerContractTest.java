package com.finfive.crisfin.domain.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finfive.crisfin.domain.analysis.dto.ApplicantProfile;
import com.finfive.crisfin.domain.crisis.CrisisType;
import com.finfive.crisfin.domain.recommendation.rule.BenefitCriteriaFixtures;
import com.finfive.crisfin.domain.recommendation.rule.BenefitRuleEngine;
import com.finfive.crisfin.domain.recommendation.timeline.TimelineBuilder;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the assembled analysis result matches the shape the front-end expects
 * (frontend/src/lib/types.ts: ReceivableItem, TimelinePhase/TimelineTask, NeedsMoreInputItem,
 * AnalysisSummary). This is the contract guard between backend and frontend — it runs without
 * a database, exercising the real rule engine + timeline + Jackson serialisation.
 */
class ResultAssemblerContractTest {

    private static final String LLM_JSON = """
            {
              "todos": [
                {"dayRange":"D+0~3","action":"진단서 발급","deadline":"퇴원 전","priority":"HIGH","reason":"청구용"},
                {"dayRange":"D+7~14","action":"실손보험 청구","deadline":"3년 이내","priority":"MED","reason":"환급"}
              ],
              "receivable": [
                {"name":"LLM이 만든 금액(폐기되어야 함)","estimatedMin":99,"estimatedMax":100,
                 "source":"x","applyUrl":"y","deadline":"z","requiredDocs":[]}
              ],
              "holdable": [],
              "actions": [
                {"name":"카드 결제 유예 신청","requiredDocs":["신분증"],"deadline":"D+15~30",
                 "contactInfo":"카드사","priority":"LOW"}
              ],
              "summary": {"totalReceivableMin":1,"totalReceivableMax":2,"urgentCount":3,"thirtyDayPlan":"plan"},
              "disclaimer": "참고용"
            }
            """;

    private final ObjectMapper mapper = new ObjectMapper();
    private final ResultAssembler assembler =
            new ResultAssembler(new BenefitRuleEngine(BenefitCriteriaFixtures.provider()), new TimelineBuilder(), mapper);

    @Test
    @SuppressWarnings("unchecked")
    void assembledResult_matchesFrontendContract() throws Exception {
        JsonNode llmNode = mapper.readTree(LLM_JSON);
        Map<String, Object> resultMap = mapper.convertValue(llmNode, HashMap.class);

        // Profile with some inputs missing → at least one NEEDS_MORE_INPUT receivable.
        ApplicantProfile profile = ApplicantProfile.builder()
                .householdSize(2)
                .monthlyIncome(2_000_000L)
                .annualOutOfPocketMedical(5_000_000L)
                // liquidFinancialAssets omitted → 긴급복지 NEEDS_MORE_INPUT
                .build();

        assembler.enrich(resultMap, llmNode, CrisisType.HOSPITALIZATION, profile);

        JsonNode result = mapper.valueToTree(resultMap);

        // ── receivable[] ──
        JsonNode receivable = result.get("receivable");
        assertThat(receivable.isArray()).isTrue();
        assertThat(receivable).isNotEmpty();
        for (JsonNode item : receivable) {
            assertThat(item.has("name")).isTrue();
            assertThat(item.has("estimatedMin")).isTrue();   // present, may be null
            assertThat(item.has("estimatedMax")).isTrue();
            assertThat(item.get("status").asText()).isIn("ELIGIBLE", "NEEDS_MORE_INPUT");
            assertThat(item.has("basis")).isTrue();
            assertThat(item.has("source")).isTrue();
            assertThat(item.has("applyUrl")).isTrue();
            assertThat(item.has("deadline")).isTrue();
            assertThat(item.get("requiredDocs").isArray()).isTrue();
        }
        // LLM amounts must be discarded (no 99/100 leak)
        assertThat(receivable.toString()).doesNotContain("\"estimatedMin\":99");
        // At least one NEEDS_MORE_INPUT with null amount
        boolean hasNeedsInput = false;
        for (JsonNode item : receivable) {
            if ("NEEDS_MORE_INPUT".equals(item.get("status").asText())) {
                assertThat(item.get("estimatedMin").isNull()).isTrue();
                hasNeedsInput = true;
            }
        }
        assertThat(hasNeedsInput).isTrue();

        // ── needsMoreInput[] ──
        JsonNode needs = result.get("needsMoreInput");
        assertThat(needs.isArray()).isTrue();
        assertThat(needs).isNotEmpty();
        for (JsonNode n : needs) {
            assertThat(n.has("benefitName")).isTrue();
            assertThat(n.get("missingInputs").isArray()).isTrue();
        }

        // ── timeline[] (urgency-sorted buckets) ──
        JsonNode timeline = result.get("timeline");
        assertThat(timeline.isArray()).isTrue();
        assertThat(timeline).isNotEmpty();
        for (JsonNode phase : timeline) {
            assertThat(phase.has("range")).isTrue();
            assertThat(phase.has("fromDay")).isTrue();
            assertThat(phase.has("toDay")).isTrue();
            JsonNode items = phase.get("items");
            assertThat(items.isArray()).isTrue();
            int prevScore = Integer.MAX_VALUE;
            for (JsonNode t : items) {
                assertThat(t.has("title")).isTrue();
                assertThat(t.get("priority").asText()).isIn("HIGH", "MED", "LOW");
                assertThat(t.has("dayRange")).isTrue();
                assertThat(t.has("urgencyScore")).isTrue();
                assertThat(t.has("category")).isTrue();
                // sorted descending by urgencyScore within a bucket
                assertThat(t.get("urgencyScore").asInt()).isLessThanOrEqualTo(prevScore);
                prevScore = t.get("urgencyScore").asInt();
            }
        }

        // ── summary totals come from the rule engine, not the LLM (was 1/2) ──
        JsonNode summary = result.get("summary");
        assertThat(summary.has("totalReceivableMin")).isTrue();
        assertThat(summary.has("totalReceivableMax")).isTrue();
        assertThat(summary.get("totalReceivableMin").asLong()).isNotEqualTo(1L);
    }
}
