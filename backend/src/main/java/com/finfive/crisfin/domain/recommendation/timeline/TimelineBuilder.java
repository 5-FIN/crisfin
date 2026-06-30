package com.finfive.crisfin.domain.recommendation.timeline;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the 30-day, urgency-sorted timeline from the LLM strategy output.
 *
 * <p>The LLM produces {@code todos} (with {@code dayRange}) and {@code actions} (with
 * {@code deadline}); this component buckets them into 0-3 / 4-14 / 15-30 day phases and
 * ranks each bucket by an urgency score derived from priority and earliness. The front-end
 * renders the buckets as-is ("백엔드가 긴급도 정렬해 줌").</p>
 */
@Component
public class TimelineBuilder {

    private static final Pattern NUMBER = Pattern.compile("\\d+");

    private record Bucket(String range, int fromDay, int toDay) {
    }

    private static final List<Bucket> BUCKETS = List.of(
            new Bucket("0-3일", 0, 3),
            new Bucket("4-14일", 4, 14),
            new Bucket("15-30일", 15, 30)
    );

    public List<TimelinePhase> build(JsonNode result) {
        List<List<TimelineTask>> grouped = List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        if (result != null) {
            for (JsonNode todo : result.path("todos")) {
                addTask(grouped,
                        text(todo, "action"),
                        text(todo, "priority"),
                        text(todo, "dayRange"),
                        "할 일");
            }
            for (JsonNode action : result.path("actions")) {
                String deadline = text(action, "deadline");
                addTask(grouped,
                        text(action, "name"),
                        text(action, "priority"),
                        deadline.isBlank() ? "30일 내" : deadline,
                        "행동");
            }
        }

        List<TimelinePhase> phases = new ArrayList<>();
        for (int i = 0; i < BUCKETS.size(); i++) {
            List<TimelineTask> items = grouped.get(i);
            if (items.isEmpty()) {
                continue;
            }
            items.sort(Comparator.comparingInt(TimelineTask::urgencyScore).reversed());
            Bucket b = BUCKETS.get(i);
            phases.add(new TimelinePhase(b.range(), b.fromDay(), b.toDay(), items));
        }
        return phases;
    }

    private void addTask(List<List<TimelineTask>> grouped,
                         String title, String priority, String dayRange, String category) {
        if (title == null || title.isBlank()) {
            return;
        }
        String normalizedPriority = normalizePriority(priority);
        int maxDay = maxDay(dayRange);
        int bucketIndex = bucketIndexForDay(maxDay);
        int urgency = urgencyScore(normalizedPriority, maxDay);
        grouped.get(bucketIndex).add(
                new TimelineTask(title.trim(), normalizedPriority, dayRange.trim(), urgency, category));
    }

    private int bucketIndexForDay(int day) {
        if (day <= 3) return 0;
        if (day <= 14) return 1;
        return 2;
    }

    /** Urgency = priority weight + earliness bonus (sooner ⇒ higher). */
    private int urgencyScore(String priority, int maxDay) {
        int base = switch (priority) {
            case "HIGH" -> 90;
            case "MED" -> 60;
            default -> 30;
        };
        int earliness = Math.max(0, 30 - maxDay) / 3; // 0..10
        return base + earliness;
    }

    /** Largest day number found in the range string; defaults to 14 (mid bucket) when absent. */
    private int maxDay(String range) {
        if (range == null) {
            return 14;
        }
        Matcher m = NUMBER.matcher(range);
        int max = -1;
        while (m.find()) {
            max = Math.max(max, Integer.parseInt(m.group()));
        }
        return max >= 0 ? max : 14;
    }

    private String normalizePriority(String priority) {
        if (priority == null) {
            return "MED";
        }
        String p = priority.trim().toUpperCase();
        return switch (p) {
            case "HIGH", "MED", "LOW" -> p;
            default -> "MED";
        };
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? "" : v.asText("");
    }
}
