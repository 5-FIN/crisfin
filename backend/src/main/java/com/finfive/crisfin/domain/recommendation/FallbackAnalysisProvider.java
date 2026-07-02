package com.finfive.crisfin.domain.recommendation;

import com.finfive.crisfin.domain.crisis.CrisisType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic, rule-based analysis skeleton used when every LLM provider fails.
 *
 * <p>Per the security spec (slide 7 "Fallback"), an LLM outage must degrade gracefully to a
 * rule-based checklist plus official-agency contacts rather than surfacing an error. Amounts
 * are still filled by {@link ResultAssembler}; this provider supplies safe, generic
 * todos/actions/holdable and a notice. Contacts are official hotlines only (harness allowlist).</p>
 */
@Component
public class FallbackAnalysisProvider {

    /**
     * Builds a fallback result map (mutable) for the crisis type. {@code receivable},
     * {@code needsMoreInput}, {@code summary} totals and {@code timeline} are added later by
     * {@link ResultAssembler#enrich}.
     */
    public Map<String, Object> build(CrisisType crisisType) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todos", commonTodos(crisisType));
        result.put("actions", commonActions(crisisType));
        result.put("holdable", holdable());
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("urgentCount", 1);
        summary.put("thirtyDayPlan", "AI 분석 서버가 일시적으로 불가하여 규칙 기반 기본 안내를 제공합니다. "
                + "받을 수 있는 금액은 아래 '받을 돈'을 확인하고, 세부 자격은 각 기관에 직접 문의하세요.");
        result.put("summary", summary);
        result.put("disclaimer", "본 안내는 AI 분석 없이 제공된 규칙 기반 기본 정보입니다. "
                + "정확한 자격·금액·기한은 관할 기관(주민센터·보건복지상담센터 129 등)에 반드시 확인하세요.");
        return result;
    }

    private List<Map<String, Object>> commonTodos(CrisisType crisisType) {
        return List.of(
                todo("D+1~3", "거주지 주민센터 또는 보건복지상담센터(129)에 긴급복지 지원 대상 여부를 문의",
                        "가능한 빨리", "HIGH", "위기 상황 초기 대응이 지원 가능성을 높입니다."),
                todo("D+1~7", crisisSpecificStep(crisisType), "상황 발생 후 가능한 빨리", "HIGH",
                        "제도별 신청 기한이 있으니 조기 확인이 필요합니다."),
                todo("D+7~30", "받을 수 있는 지원금과 미룰 수 있는 납부 항목을 정리해 우선순위 계획 수립",
                        "30일 이내", "MED", "현금 흐름을 확보하고 연체를 예방합니다.")
        );
    }

    private List<Map<String, Object>> commonActions(CrisisType crisisType) {
        return List.of(
                action("긴급복지 지원 상담", List.of("신분증", "위기상황 증빙"), "가능한 빨리",
                        "보건복지상담센터 129", "HIGH"),
                action(crisisSpecificAgencyAction(crisisType), List.of("신분증", "관련 증빙"), "상황 발생 후 확인",
                        crisisSpecificAgencyContact(crisisType), "MED")
        );
    }

    private List<Map<String, Object>> holdable() {
        return List.of(
                Map.of("name", "대출 원리금 상환", "deferPeriod", "기관별 상이(확인 필요)", "riskLevel", "MED",
                        "howTo", "거래 금융기관에 채무조정·상환유예 가능 여부를 문의하세요.",
                        "caution", "유예해도 이자는 계속 발생할 수 있으니 조건을 확인하세요."),
                Map.of("name", "공과금·보험료", "deferPeriod", "기관별 상이(확인 필요)", "riskLevel", "LOW",
                        "howTo", "해당 기관에 납부 유예·분납 제도를 문의하세요.",
                        "caution", "연체 처리되지 않도록 유예 신청 여부를 확인하세요.")
        );
    }

    private String crisisSpecificStep(CrisisType crisisType) {
        return switch (crisisType) {
            case UNEMPLOYMENT -> "고용센터 또는 고용노동부(1350)에 실업급여 수급자격을 확인";
            case HOSPITALIZATION -> "국민건강보험공단(1577-1000)에 본인부담상한제·의료비 지원을 확인";
            case ACCIDENT -> "근로복지공단(1588-0075)에 산재보험 요양·휴업급여 대상 여부를 확인";
            case CAREGIVING -> "국민건강보험공단(1577-1000)에 노인장기요양 등급·급여를 확인";
            case BEREAVEMENT -> "정부24 또는 주민센터에 안심상속 원스톱 서비스를 신청";
        };
    }

    private String crisisSpecificAgencyAction(CrisisType crisisType) {
        return switch (crisisType) {
            case UNEMPLOYMENT -> "실업급여 수급자격 확인";
            case HOSPITALIZATION, CAREGIVING -> "건강보험 급여·의료비 지원 확인";
            case ACCIDENT -> "산재보험 급여 신청 확인";
            case BEREAVEMENT -> "안심상속 원스톱 서비스 신청";
        };
    }

    private String crisisSpecificAgencyContact(CrisisType crisisType) {
        return switch (crisisType) {
            case UNEMPLOYMENT -> "고용노동부 1350";
            case HOSPITALIZATION, CAREGIVING -> "국민건강보험공단 1577-1000";
            case ACCIDENT -> "근로복지공단 1588-0075";
            case BEREAVEMENT -> "정부민원안내 110";
        };
    }

    private Map<String, Object> todo(String dayRange, String action, String deadline, String priority, String reason) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("dayRange", dayRange);
        t.put("action", action);
        t.put("deadline", deadline);
        t.put("priority", priority);
        t.put("reason", reason);
        return t;
    }

    private Map<String, Object> action(String name, List<String> docs, String deadline, String contact, String priority) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("name", name);
        a.put("requiredDocs", docs);
        a.put("deadline", deadline);
        a.put("contactInfo", contact);
        a.put("priority", priority);
        return a;
    }
}
