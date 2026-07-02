package com.finfive.crisfin.domain.recommendation.harness;

import com.finfive.crisfin.domain.crisis.CrisisType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the hallucination harness — deterministic verifiers + orchestration.
 */
class AnalysisHarnessTest {

    private final ContactVerifier contact = new ContactVerifier();
    private final AmountLeakVerifier amount = new AmountLeakVerifier();
    private final PhrasingVerifier phrasing = new PhrasingVerifier();
    private final DateVerifier date = new DateVerifier();
    private final AnalysisHarness harness =
            new AnalysisHarness(List.of(contact, amount, phrasing, date));

    private final HarnessContext ctx = new HarnessContext(CrisisType.UNEMPLOYMENT, Set.of());

    private Map<String, Object> action(String name, String contactInfo, String deadline) {
        Map<String, Object> a = new HashMap<>();
        a.put("name", name);
        a.put("contactInfo", contactInfo);
        a.put("deadline", deadline);
        a.put("priority", "HIGH");
        return a;
    }

    private Map<String, Object> todo(String action, String dayRange, String reason) {
        Map<String, Object> t = new HashMap<>();
        t.put("action", action);
        t.put("dayRange", dayRange);
        t.put("reason", reason);
        t.put("priority", "HIGH");
        return t;
    }

    private Map<String, Object> resultWith(List<Map<String, Object>> actions, List<Map<String, Object>> todos) {
        Map<String, Object> m = new HashMap<>();
        m.put("actions", new ArrayList<>(actions));
        m.put("todos", new ArrayList<>(todos));
        return m;
    }

    @Test
    void fabricatedPhone_isHardAndStripped() {
        Map<String, Object> result = resultWith(
                List.of(action("실업급여 신청", "고용센터 02-9999-1234로 문의", "D+14")), List.of());

        List<HarnessFlag> detected = harness.detect(result, ctx);
        assertThat(detected).anyMatch(f -> f.type().equals("FABRICATED_CONTACT") && f.severity() == Severity.HARD);
        assertThat(AnalysisHarness.hasHard(detected)).isTrue();

        List<HarnessFlag> sanitized = harness.sanitize(result, ctx);
        assertThat(sanitized).anyMatch(f -> f.action().equals("STRIPPED"));
        @SuppressWarnings("unchecked")
        String cleaned = (String) ((Map<String, Object>) ((List<?>) result.get("actions")).get(0)).get("contactInfo");
        assertThat(cleaned).doesNotContain("02-9999-1234");
    }

    @Test
    void officialHotline_passes() {
        Map<String, Object> result = resultWith(
                List.of(action("긴급복지 문의", "보건복지상담센터 129", "D+3")), List.of());

        List<HarnessFlag> detected = harness.detect(result, ctx);
        assertThat(detected).noneMatch(f -> f.type().equals("FABRICATED_CONTACT"));
    }

    @Test
    void amountInNarrative_isHardAndRedacted() {
        Map<String, Object> result = resultWith(List.of(),
                List.of(todo("실업급여 신청하면 매월 198만원 받습니다", "D+1~3", "비자발적 이직")));

        List<HarnessFlag> detected = harness.detect(result, ctx);
        assertThat(detected).anyMatch(f -> f.type().equals("AMOUNT_LEAK") && f.severity() == Severity.HARD);

        harness.sanitize(result, ctx);
        @SuppressWarnings("unchecked")
        String cleaned = (String) ((Map<String, Object>) ((List<?>) result.get("todos")).get(0)).get("action");
        assertThat(cleaned).doesNotContain("198만원");
    }

    @Test
    void nonMonetaryNumbers_areNotFlagged() {
        // 3개월 / 180일 / 60% 는 금액이 아니므로 통과해야 한다
        Map<String, Object> result = resultWith(List.of(),
                List.of(todo("고용보험 180일 이상이면 평균임금 60% 수준 지원 대상", "D+1", "자격 확인")));

        List<HarnessFlag> detected = harness.detect(result, ctx);
        assertThat(detected).noneMatch(f -> f.type().equals("AMOUNT_LEAK"));
    }

    @Test
    void overconfidentWording_isSoftFlagOnly() {
        Map<String, Object> result = resultWith(List.of(),
                List.of(todo("신청하면 반드시 받을 수 있습니다", "D+1", "확실히 지급됩니다")));

        List<HarnessFlag> detected = harness.detect(result, ctx);
        assertThat(detected).anyMatch(f -> f.type().equals("OVERCONFIDENT") && f.severity() == Severity.SOFT);
        assertThat(AnalysisHarness.hasHard(detected)).isFalse();
    }

    @Test
    void absoluteDate_isSoftFlag() {
        Map<String, Object> result = resultWith(
                List.of(action("신청", "129", "2023-10-15까지")), List.of());

        List<HarnessFlag> detected = harness.detect(result, ctx);
        assertThat(detected).anyMatch(f -> f.type().equals("ABSOLUTE_DATE") && f.severity() == Severity.SOFT);
    }

    @Test
    void cleanResult_hasNoFlags() {
        Map<String, Object> result = resultWith(
                List.of(action("실업급여 수급자격 신청", "고용노동부 상담 1350", "D+14")),
                List.of(todo("워크넷 구직등록", "D+1~3", "실업급여 신청 전제조건")));

        List<HarnessFlag> detected = harness.detect(result, ctx);
        assertThat(detected).isEmpty();
    }

    @Test
    void repairFeedback_listsHardViolationsOnly() {
        List<HarnessFlag> flags = List.of(
                HarnessFlag.hard("actions[0].contactInfo", "FABRICATED_CONTACT", "가짜 번호"),
                HarnessFlag.soft("todos[0].action", "OVERCONFIDENT", "단정 표현"));

        String feedback = harness.buildRepairFeedback(flags);
        assertThat(feedback).contains("actions[0].contactInfo");
        assertThat(feedback).doesNotContain("todos[0].action"); // SOFT 는 재생성 피드백에서 제외
    }
}
