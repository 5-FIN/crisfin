package com.finfive.crisfin.domain.recommendation.timeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TimelineBuilder} — 버킷 분류 + 긴급도 정렬.
 */
class TimelineBuilderTest {

    private final TimelineBuilder builder = new TimelineBuilder();
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode json(String s) {
        try {
            return mapper.readTree(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void build_null_returnsEmpty() {
        assertThat(builder.build(null)).isEmpty();
    }

    @Test
    void build_emptyResult_returnsEmpty() {
        assertThat(builder.build(json("{}"))).isEmpty();
    }

    @Test
    void build_bucketsTodosAndActionsByDay() {
        JsonNode result = json("""
                {
                  "todos": [
                    { "action": "실업급여 신청", "priority": "HIGH", "dayRange": "1-3일" }
                  ],
                  "actions": [
                    { "name": "국민취업지원제도 등록", "priority": "MED", "deadline": "" }
                  ]
                }
                """);

        List<TimelinePhase> phases = builder.build(result);

        // 0-3일 버킷(todo) + 15-30일 버킷(deadline 없는 action → "30일 내")
        assertThat(phases).hasSize(2);
        assertThat(phases.get(0).range()).isEqualTo("0-3일");
        assertThat(phases.get(0).items()).extracting(TimelineTask::title)
                .containsExactly("실업급여 신청");
        assertThat(phases.get(phases.size() - 1).range()).isEqualTo("15-30일");
    }

    @Test
    void build_sortsBucketByUrgencyDescending() {
        JsonNode result = json("""
                {
                  "todos": [
                    { "action": "낮은 우선순위", "priority": "LOW",  "dayRange": "2-3일" },
                    { "action": "높은 우선순위", "priority": "HIGH", "dayRange": "1-3일" }
                  ]
                }
                """);

        List<TimelinePhase> phases = builder.build(result);

        assertThat(phases).hasSize(1);
        // HIGH가 LOW보다 먼저 와야 한다
        assertThat(phases.get(0).items()).extracting(TimelineTask::priority)
                .containsExactly("HIGH", "LOW");
        assertThat(phases.get(0).items().get(0).urgencyScore())
                .isGreaterThan(phases.get(0).items().get(1).urgencyScore());
    }

    @Test
    void build_normalizesLowercasePriority() {
        JsonNode result = json("""
                {
                  "todos": [ { "action": "할 일", "priority": "high", "dayRange": "1-2일" } ]
                }
                """);

        List<TimelinePhase> phases = builder.build(result);

        assertThat(phases.get(0).items().get(0).priority()).isEqualTo("HIGH");
    }

    @Test
    void build_blankTitle_isSkipped() {
        JsonNode result = json("""
                {
                  "todos": [ { "action": "", "priority": "HIGH", "dayRange": "1-3일" } ]
                }
                """);

        assertThat(builder.build(result)).isEmpty();
    }
}
