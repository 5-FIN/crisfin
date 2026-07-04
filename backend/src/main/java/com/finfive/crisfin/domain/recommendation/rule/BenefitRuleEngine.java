package com.finfive.crisfin.domain.recommendation.rule;

import com.finfive.crisfin.domain.analysis.dto.ApplicantProfile;
import com.finfive.crisfin.domain.crisis.CrisisType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic eligibility / amount estimator for Korean public benefits.
 *
 * <p>This is the authority for receivable amounts — the LLM is explicitly forbidden from
 * generating amounts, so all {@code estimatedMin/Max} values originate here. When required
 * applicant inputs are missing, the corresponding benefit is emitted with
 * {@code NEEDS_MORE_INPUT} status (null amounts) and recorded in {@code needsMoreInput} so the
 * UI can prompt for the missing fields.</p>
 *
 * <p>Threshold figures are approximate 2024 references and are intended as guidance only;
 * actual entitlements are determined by the issuing authority.</p>
 */
@Component
public class BenefitRuleEngine {

    private final BenefitCriteriaProvider criteria;

    /**
     * Demo mode (mock/local-mock only): when {@code true}, every crisis type is guaranteed to
     * surface concrete receivable amounts + 지원처 + 신청 링크 so the demo shows figures on all
     * screens. Defaults to {@code false} so production and unit tests keep the input-driven,
     * {@code NEEDS_MORE_INPUT}-when-uncertain behaviour. Enabled in {@code application-local-mock.yml}.
     */
    @Value("${analysis.demo-amounts:false}")
    private boolean demoAmounts;

    public BenefitRuleEngine(BenefitCriteriaProvider criteria) {
        this.criteria = criteria;
    }

    public RuleEvaluation evaluate(CrisisType crisisType, ApplicantProfile profile) {
        if (demoAmounts) {
            return demoEvaluation(crisisType);
        }

        ApplicantProfile p = (profile != null) ? profile : ApplicantProfile.builder().build();

        List<ReceivableEstimate> receivables = new ArrayList<>();
        List<NeedsMoreInputItem> needsMoreInput = new ArrayList<>();

        // Crisis-specific public benefit
        switch (crisisType) {
            case HOSPITALIZATION -> outOfPocketCap(p, receivables, needsMoreInput);
            case UNEMPLOYMENT -> unemploymentBenefit(p, receivables, needsMoreInput);
            case CAREGIVING -> longTermCare(p, receivables, needsMoreInput);
            case ACCIDENT -> industrialAccident(receivables, needsMoreInput);
            case BEREAVEMENT -> inheritanceOneStop(receivables, needsMoreInput);
        }

        // 긴급복지 생계지원 applies across crisis types
        emergencySupport(p, receivables, needsMoreInput);

        long totalMin = receivables.stream()
                .filter(r -> r.estimatedMin() != null).mapToLong(ReceivableEstimate::estimatedMin).sum();
        long totalMax = receivables.stream()
                .filter(r -> r.estimatedMax() != null).mapToLong(ReceivableEstimate::estimatedMax).sum();

        return new RuleEvaluation(receivables, needsMoreInput, totalMin, totalMax);
    }

    // ------------------------------------------------------------------ //
    //  Demo mode (mock/local-mock) — guaranteed amounts for every crisis
    // ------------------------------------------------------------------ //

    /**
     * Builds a fully-populated, all-{@code ELIGIBLE} evaluation for the demo/mock profile so
     * every crisis type shows 받을 금액·지원처·신청 링크 on all screens (사고/재해·가족 사망 포함,
     * which the input-driven path leaves as {@code NEEDS_MORE_INPUT}). Figures are illustrative
     * demo estimates; 지원처·신청 URL·서류·기한 reuse the same official references as the real rules.
     */
    private RuleEvaluation demoEvaluation(CrisisType crisisType) {
        List<ReceivableEstimate> receivables = new ArrayList<>();

        switch (crisisType) {
            case HOSPITALIZATION -> receivables.add(ReceivableEstimate.eligible(
                    "건강보험 본인부담상한제 환급", 800_000, 1_500_000,
                    "연간 본인부담 의료비가 소득분위 상한을 초과해 차액을 환급받는 예시입니다. (데모 추정치)",
                    "국민건강보험공단", "https://www.nhis.or.kr", "지급 결정일로부터 3년",
                    List.of("진료비 영수증", "본인 명의 계좌")));
            case UNEMPLOYMENT -> receivables.add(ReceivableEstimate.eligible(
                    "실업급여(구직급여)", 5_400_000, 8_100_000,
                    "고용보험 가입·비자발적 이직 요건 충족 시 1일 구직급여 × 120~270일 기준 예시입니다. (데모 추정치)",
                    "고용노동부", "https://www.ei.go.kr", "퇴직 다음 날부터 12개월 이내",
                    List.of("이직확인서", "고용보험 피보험자격 이력")));
            case CAREGIVING -> receivables.add(ReceivableEstimate.eligible(
                    "재난적의료비 지원", 2_000_000, 5_000_000,
                    "소득 기준 충족 시 과다한 본인부담 의료비의 일부를 지원받는 예시입니다. (데모 추정치)",
                    "국민건강보험공단", "https://www.nhis.or.kr", "진료 종료 후 180일 이내",
                    List.of("진단서", "진료비 영수증", "소득·재산 증빙")));
            case ACCIDENT -> receivables.add(ReceivableEstimate.eligible(
                    "산업재해보상보험 급여", 3_000_000, 9_000_000,
                    "업무상 재해 인정 시 요양급여·휴업급여(평균임금 70%) 등을 받는 예시입니다. (데모 추정치)",
                    "근로복지공단", "https://www.comwel.or.kr", "요양 개시 후 3년",
                    List.of("산재 요양급여 신청서", "재해 경위서")));
            case BEREAVEMENT -> receivables.add(ReceivableEstimate.eligible(
                    "안심상속 원스톱 서비스", 2_000_000, 6_000_000,
                    "안심상속 통합조회로 확인된 사망자의 예금·보험·연금 등 유족 수령 예시입니다. (데모 추정치)",
                    "행정안전부", "https://www.gov.kr", "사망일이 속한 달의 말일부터 6개월 이내",
                    List.of("사망진단서", "상속인 신분증")));
        }

        // 긴급복지 생계지원 — 모든 위기유형 공통(데모에서는 대상 가정)
        receivables.add(ReceivableEstimate.eligible(
                "긴급복지 생계지원", 1_300_000, 3_900_000,
                "기준 중위소득 75% 이하·재산 기준 충족 시 생계지원 월 지급액(최대 3개월) 예시입니다. (데모 추정치)",
                "보건복지부", "https://www.bokjiro.go.kr", "위기상황 발생 후 신청",
                List.of("신분증", "금융정보 등 제공 동의서")));

        long totalMin = receivables.stream()
                .filter(r -> r.estimatedMin() != null).mapToLong(ReceivableEstimate::estimatedMin).sum();
        long totalMax = receivables.stream()
                .filter(r -> r.estimatedMax() != null).mapToLong(ReceivableEstimate::estimatedMax).sum();

        return new RuleEvaluation(receivables, new ArrayList<>(), totalMin, totalMax);
    }

    // ------------------------------------------------------------------ //
    //  Individual benefit rules
    // ------------------------------------------------------------------ //

    /** 본인부담상한제 — 연간 본인부담 의료비가 소득분위별 상한을 초과하면 환급. */
    private void outOfPocketCap(ApplicantProfile p,
                                List<ReceivableEstimate> out,
                                List<NeedsMoreInputItem> needs) {
        String name = "건강보험 본인부담상한제 환급";
        String url = "https://www.nhis.or.kr";
        List<String> docs = List.of("진료비 영수증", "본인 명의 계좌");

        List<String> missing = new ArrayList<>();
        if (p.getAnnualOutOfPocketMedical() == null) missing.add("연간 본인부담 의료비");
        if (p.getMonthlyIncome() == null) missing.add("월 소득(소득분위 산정용)");
        if (!missing.isEmpty()) {
            out.add(ReceivableEstimate.needsMoreInput(name,
                    "소득분위별 상한 초과분을 환급받을 수 있습니다. 자격·금액 산정에 추가 정보가 필요합니다.",
                    "국민건강보험공단", url, "지급 결정일로부터 3년", docs));
            needs.add(new NeedsMoreInputItem(name, missing));
            return;
        }

        long cap = criteria.outOfPocketCap(p.getMonthlyIncome());
        long oop = p.getAnnualOutOfPocketMedical();
        long refund = Math.max(0, oop - cap);
        if (refund > 0) {
            out.add(ReceivableEstimate.eligible(name, refund, refund,
                    String.format("연간 본인부담 의료비 %,d원이 소득분위 상한 %,d원을 초과해 차액 환급 대상입니다.", oop, cap),
                    "국민건강보험공단", url, "지급 결정일로부터 3년", docs));
        }
    }

    /** 실업급여(구직급여) — 고용보험 180일 이상 + 비자발적 이직. */
    private void unemploymentBenefit(ApplicantProfile p,
                                     List<ReceivableEstimate> out,
                                     List<NeedsMoreInputItem> needs) {
        String name = "실업급여(구직급여)";
        String url = "https://www.ei.go.kr";
        List<String> docs = List.of("이직확인서", "고용보험 피보험자격 이력");

        List<String> missing = new ArrayList<>();
        if (p.getEmploymentInsuranceMonths() == null) missing.add("고용보험 가입기간(개월)");
        if (p.getInvoluntarySeparation() == null) missing.add("비자발적 이직 여부");
        if (p.getMonthlyIncome() == null) missing.add("월 소득(평균임금 산정용)");
        if (!missing.isEmpty()) {
            out.add(ReceivableEstimate.needsMoreInput(name,
                    "고용보험 가입기간·이직 사유·소득에 따라 자격과 금액이 정해집니다.",
                    "고용노동부", url, "퇴직 다음 날부터 12개월 이내", docs));
            needs.add(new NeedsMoreInputItem(name, missing));
            return;
        }

        boolean eligible = p.getEmploymentInsuranceMonths() >= 6 && Boolean.TRUE.equals(p.getInvoluntarySeparation());
        if (!eligible) {
            return; // 자발적 이직 또는 가입기간 미달 — 비대상
        }

        long daily = Math.round(p.getMonthlyIncome() / 30.0 * 0.6);
        daily = Math.max(criteria.uiDailyMin(), Math.min(daily, criteria.uiDailyMax()));
        long min = daily * 120; // 최소 소정급여일수
        long max = daily * 270; // 최대 소정급여일수
        out.add(ReceivableEstimate.eligible(name, min, max,
                String.format("고용보험 %d개월 가입·비자발적 이직 요건 충족. 1일 구직급여 약 %,d원 × 120~270일 기준 추정.",
                        p.getEmploymentInsuranceMonths(), daily),
                "고용노동부", url, "퇴직 다음 날부터 12개월 이내", docs));
    }

    /** 장기요양 급여 — 등급별 재가급여 월 한도의 본인부담(15%) 제외 지원액. */
    private void longTermCare(ApplicantProfile p,
                              List<ReceivableEstimate> out,
                              List<NeedsMoreInputItem> needs) {
        String name = "노인장기요양보험 급여";
        String url = "https://www.longtermcare.or.kr";
        List<String> docs = List.of("장기요양인정서", "표준장기요양이용계획서");

        if (p.getCareGrade() == null) {
            out.add(ReceivableEstimate.needsMoreInput(name,
                    "장기요양 등급(1~5)에 따라 월 한도와 지원액이 결정됩니다.",
                    "국민건강보험공단", url, "등급 판정 후 수시", docs));
            needs.add(new NeedsMoreInputItem(name, List.of("장기요양 등급")));
            return;
        }

        long limit = criteria.careLimit(p.getCareGrade());
        if (limit <= 0) {
            return;
        }
        long support = Math.round(limit * 0.85); // 본인부담 15% 제외
        out.add(ReceivableEstimate.eligible(name, support, support,
                String.format("장기요양 %d등급 재가급여 월 한도 %,d원 기준, 본인부담 15%% 제외 약 %,d원 지원(월).",
                        p.getCareGrade(), limit, support),
                "국민건강보험공단", url, "등급 판정 후 수시", docs));
    }

    /** 산재보험 — 업무상 재해 여부 등 추가 확인 필요. */
    private void industrialAccident(List<ReceivableEstimate> out, List<NeedsMoreInputItem> needs) {
        String name = "산업재해보상보험 급여";
        out.add(ReceivableEstimate.needsMoreInput(name,
                "업무상 재해로 인정되면 요양급여·휴업급여(평균임금 70%) 등을 받을 수 있습니다. 재해 경위 확인이 필요합니다.",
                "근로복지공단", "https://www.comwel.or.kr", "요양 개시 후 3년",
                List.of("산재 요양급여 신청서", "재해 경위서")));
        needs.add(new NeedsMoreInputItem(name, List.of("업무상 재해 여부", "평균임금")));
    }

    /** 안심상속 원스톱 — 사망자 금융재산 일괄조회(금액은 조회 후 산정). */
    private void inheritanceOneStop(List<ReceivableEstimate> out, List<NeedsMoreInputItem> needs) {
        String name = "안심상속 원스톱 서비스";
        out.add(ReceivableEstimate.needsMoreInput(name,
                "사망자의 금융재산·보험·연금을 일괄 조회할 수 있습니다. 조회 결과에 따라 수령액이 산정됩니다.",
                "행정안전부", "https://www.gov.kr", "사망일이 속한 달의 말일부터 6개월 이내",
                List.of("사망진단서", "상속인 신분증")));
        needs.add(new NeedsMoreInputItem(name, List.of("상속 금융재산 조회 결과")));
    }

    /** 긴급복지 생계지원 — 위기상황 + 소득·재산 기준 충족 시. */
    private void emergencySupport(ApplicantProfile p,
                                  List<ReceivableEstimate> out,
                                  List<NeedsMoreInputItem> needs) {
        String name = "긴급복지 생계지원";
        String url = "https://www.bokjiro.go.kr";
        List<String> docs = List.of("신분증", "금융정보 등 제공 동의서");

        List<String> missing = new ArrayList<>();
        if (p.getHouseholdSize() == null) missing.add("가구원 수");
        if (p.getMonthlyIncome() == null) missing.add("최근 월 소득");
        if (p.getLiquidFinancialAssets() == null) missing.add("가구 금융재산");
        if (!missing.isEmpty()) {
            out.add(ReceivableEstimate.needsMoreInput(name,
                    "기준 중위소득 75% 이하·재산 기준 충족 시 생계지원을 받을 수 있습니다.",
                    "보건복지부", url, "위기상황 발생 후 신청", docs));
            needs.add(new NeedsMoreInputItem(name, missing));
            return;
        }

        long incomeThreshold = Math.round(criteria.medianIncome(p.getHouseholdSize()) * 0.75);
        boolean incomeOk = p.getMonthlyIncome() <= incomeThreshold;
        boolean assetOk = p.getLiquidFinancialAssets() <= criteria.emergencyAssetCap();
        if (!incomeOk || !assetOk) {
            return; // 소득/재산 기준 초과 — 비대상
        }

        long monthly = criteria.emergencySupport(p.getHouseholdSize());
        long min = monthly;        // 1개월
        long max = monthly * 3L;   // 최대 3개월(연장 가정)
        out.add(ReceivableEstimate.eligible(name, min, max,
                String.format("가구원 %d인 기준 중위소득 75%%(%,d원) 이하 요건 충족. 생계지원 월 %,d원(최대 3개월) 기준 추정.",
                        p.getHouseholdSize(), incomeThreshold, monthly),
                "보건복지부", url, "위기상황 발생 후 신청", docs));
    }
}
