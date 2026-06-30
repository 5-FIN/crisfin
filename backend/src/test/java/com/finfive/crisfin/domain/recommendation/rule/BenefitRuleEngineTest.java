package com.finfive.crisfin.domain.recommendation.rule;

import com.finfive.crisfin.domain.analysis.dto.ApplicantProfile;
import com.finfive.crisfin.domain.crisis.CrisisType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the deterministic {@link BenefitRuleEngine}.
 */
class BenefitRuleEngineTest {

    private final BenefitRuleEngine engine =
            new BenefitRuleEngine(BenefitCriteriaFixtures.provider());

    @Test
    void unemployment_eligible_producesAmountRange() {
        ApplicantProfile profile = ApplicantProfile.builder()
                .householdSize(2)
                .monthlyIncome(2_500_000L)
                .employmentInsuranceMonths(24)
                .involuntarySeparation(true)
                .liquidFinancialAssets(5_000_000L)
                .build();

        RuleEvaluation eval = engine.evaluate(CrisisType.UNEMPLOYMENT, profile);

        ReceivableEstimate ui = eval.receivables().stream()
                .filter(r -> r.name().contains("실업급여"))
                .findFirst().orElseThrow();

        assertThat(ui.status()).isEqualTo(ReceivableEstimate.ELIGIBLE);
        assertThat(ui.estimatedMin()).isNotNull().isPositive();
        assertThat(ui.estimatedMax()).isNotNull();
        assertThat(ui.estimatedMax()).isGreaterThan(ui.estimatedMin());
        assertThat(eval.totalReceivableMin()).isPositive();
    }

    @Test
    void unemployment_missingInputs_marksNeedsMoreInput() {
        // Only crisis type known, no employment details
        RuleEvaluation eval = engine.evaluate(CrisisType.UNEMPLOYMENT, ApplicantProfile.builder().build());

        ReceivableEstimate ui = eval.receivables().stream()
                .filter(r -> r.name().contains("실업급여"))
                .findFirst().orElseThrow();

        assertThat(ui.status()).isEqualTo(ReceivableEstimate.NEEDS_MORE_INPUT);
        assertThat(ui.estimatedMin()).isNull();
        assertThat(ui.estimatedMax()).isNull();
        assertThat(eval.needsMoreInput())
                .anySatisfy(n -> assertThat(n.benefitName()).contains("실업급여"));
    }

    @Test
    void unemployment_voluntarySeparation_isNotEligible() {
        ApplicantProfile profile = ApplicantProfile.builder()
                .monthlyIncome(3_000_000L)
                .employmentInsuranceMonths(24)
                .involuntarySeparation(false)
                .build();

        RuleEvaluation eval = engine.evaluate(CrisisType.UNEMPLOYMENT, profile);

        assertThat(eval.receivables())
                .noneSatisfy(r -> assertThat(r.name()).contains("실업급여"));
    }

    @Test
    void hospitalization_outOfPocketCap_refundsExcess() {
        ApplicantProfile profile = ApplicantProfile.builder()
                .householdSize(1)
                .monthlyIncome(2_000_000L)        // 2~3분위 → 상한 1,080,000
                .liquidFinancialAssets(3_000_000L)
                .annualOutOfPocketMedical(5_000_000L)
                .build();

        RuleEvaluation eval = engine.evaluate(CrisisType.HOSPITALIZATION, profile);

        ReceivableEstimate cap = eval.receivables().stream()
                .filter(r -> r.name().contains("본인부담상한제"))
                .findFirst().orElseThrow();

        assertThat(cap.status()).isEqualTo(ReceivableEstimate.ELIGIBLE);
        assertThat(cap.estimatedMin()).isEqualTo(5_000_000L - 1_080_000L);
    }

    @Test
    void emergencySupport_overIncomeThreshold_isNotEligible() {
        ApplicantProfile profile = ApplicantProfile.builder()
                .householdSize(1)
                .monthlyIncome(9_000_000L)        // well above 75% median
                .liquidFinancialAssets(1_000_000L)
                .build();

        RuleEvaluation eval = engine.evaluate(CrisisType.UNEMPLOYMENT, profile);

        assertThat(eval.receivables())
                .noneSatisfy(r -> assertThat(r.name()).contains("긴급복지"));
    }
}
