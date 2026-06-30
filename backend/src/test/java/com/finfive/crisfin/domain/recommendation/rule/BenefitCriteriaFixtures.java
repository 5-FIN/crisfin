package com.finfive.crisfin.domain.recommendation.rule;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared test fixture for rule-engine threshold criteria.
 *
 * <p>Mirrors the {@code V8__seed_benefit_criteria.sql} rows 1:1 so unit tests can build a
 * {@link BenefitCriteriaProvider} without a database while producing the exact same deterministic
 * estimates as production. Lives in the {@code rule} package so it can reach the package-private
 * {@link BenefitCriteriaProvider} constructor on behalf of callers in other test packages.</p>
 */
public final class BenefitCriteriaFixtures {

    private BenefitCriteriaFixtures() {
    }

    /** A provider seeded with the full production criteria set. */
    public static BenefitCriteriaProvider provider() {
        return new BenefitCriteriaProvider(rows());
    }

    /** The full production criteria set as entity rows. */
    public static List<BenefitCriteria> rows() {
        List<BenefitCriteria> rows = new ArrayList<>();

        addRow(rows, "MEDIAN_INCOME", "1", 2_228_445L);
        addRow(rows, "MEDIAN_INCOME", "2", 3_682_609L);
        addRow(rows, "MEDIAN_INCOME", "3", 4_714_657L);
        addRow(rows, "MEDIAN_INCOME", "4", 5_729_913L);
        addRow(rows, "MEDIAN_INCOME", "5", 6_695_735L);
        addRow(rows, "MEDIAN_INCOME", "6", 7_618_369L);
        addRow(rows, "MEDIAN_INCOME", "EXTRA", 922_634L);

        addRow(rows, "EMERGENCY_SUPPORT", "1", 713_100L);
        addRow(rows, "EMERGENCY_SUPPORT", "2", 1_178_400L);
        addRow(rows, "EMERGENCY_SUPPORT", "3", 1_508_600L);
        addRow(rows, "EMERGENCY_SUPPORT", "4", 1_841_700L);
        addRow(rows, "EMERGENCY_SUPPORT", "5", 2_072_101L);
        addRow(rows, "EMERGENCY_SUPPORT", "6", 2_348_359L);
        addRow(rows, "EMERGENCY_SUPPORT", "EXTRA", 230_000L);

        addRow(rows, "EMERGENCY_ASSET_CAP", "CAP", 100_000_000L);

        addRow(rows, "UI_DAILY", "MAX", 66_000L);
        addRow(rows, "UI_DAILY", "MIN", 63_104L);

        addRow(rows, "OOP_CAP", "1500000", 870_000L);
        addRow(rows, "OOP_CAP", "2500000", 1_080_000L);
        addRow(rows, "OOP_CAP", "3500000", 1_550_000L);
        addRow(rows, "OOP_CAP", "5000000", 2_890_000L);
        addRow(rows, "OOP_CAP", "7000000", 3_600_000L);
        addRow(rows, "OOP_CAP", "9000000", 4_430_000L);
        addRow(rows, "OOP_CAP", "9999999999", 5_980_000L);

        addRow(rows, "CARE_LIMIT", "1", 2_069_900L);
        addRow(rows, "CARE_LIMIT", "2", 1_869_600L);
        addRow(rows, "CARE_LIMIT", "3", 1_455_800L);
        addRow(rows, "CARE_LIMIT", "4", 1_341_800L);
        addRow(rows, "CARE_LIMIT", "5", 1_151_600L);

        return rows;
    }

    private static void addRow(List<BenefitCriteria> rows, String category, String bracketKey, long amount) {
        rows.add(BenefitCriteria.builder()
                .category(category)
                .bracketKey(bracketKey)
                .amount(amount)
                .build());
    }
}
