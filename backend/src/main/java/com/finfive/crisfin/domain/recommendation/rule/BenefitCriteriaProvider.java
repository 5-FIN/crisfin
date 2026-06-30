package com.finfive.crisfin.domain.recommendation.rule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory accessor for {@link BenefitCriteria} threshold rows.
 *
 * <p>Loads the full {@code benefit_criteria} table once at construction and indexes it by
 * {@code (category, bracketKey)} so the deterministic {@link BenefitRuleEngine} can look up
 * amounts without touching the database per request. A package-private constructor that takes a
 * pre-built row list exists so unit tests can seed fixtures directly.</p>
 */
@Component
public class BenefitCriteriaProvider {

    private static final String MEDIAN_INCOME = "MEDIAN_INCOME";
    private static final String EMERGENCY_SUPPORT = "EMERGENCY_SUPPORT";
    private static final String EMERGENCY_ASSET_CAP = "EMERGENCY_ASSET_CAP";
    private static final String UI_DAILY = "UI_DAILY";
    private static final String OOP_CAP = "OOP_CAP";
    private static final String CARE_LIMIT = "CARE_LIMIT";

    private static final String EXTRA = "EXTRA";

    /** category -> (bracketKey -> amount) */
    private final Map<String, Map<String, Long>> byCategory = new HashMap<>();

    /** OOP_CAP rows sorted ascending by numeric income ceiling (bracketKey). */
    private final List<long[]> oopCapTiers = new ArrayList<>();

    @Autowired
    public BenefitCriteriaProvider(BenefitCriteriaRepository repository) {
        this(repository.findAll());
    }

    BenefitCriteriaProvider(List<BenefitCriteria> rows) {
        for (BenefitCriteria row : rows) {
            byCategory
                    .computeIfAbsent(row.getCategory(), k -> new HashMap<>())
                    .put(row.getBracketKey(), row.getAmount());
        }

        Map<String, Long> oop = byCategory.get(OOP_CAP);
        if (oop != null) {
            oop.entrySet().stream()
                    .map(e -> new long[]{Long.parseLong(e.getKey()), e.getValue()})
                    .sorted(Comparator.comparingLong((long[] t) -> t[0]))
                    .forEach(oopCapTiers::add);
        }
    }

    /** 2024 기준 중위소득 (월, 원). 6인 초과는 6인 기준 + (초과 인원 × EXTRA). */
    public long medianIncome(int householdSize) {
        if (householdSize <= 6) {
            return required(MEDIAN_INCOME, String.valueOf(Math.max(1, householdSize)));
        }
        return required(MEDIAN_INCOME, "6") + (householdSize - 6) * required(MEDIAN_INCOME, EXTRA);
    }

    /** 긴급복지 생계지원 (월, 원). 6인 초과는 6인 기준 + (초과 인원 × EXTRA). */
    public long emergencySupport(int householdSize) {
        if (householdSize <= 6) {
            return required(EMERGENCY_SUPPORT, String.valueOf(Math.max(1, householdSize)));
        }
        return required(EMERGENCY_SUPPORT, "6") + (householdSize - 6) * required(EMERGENCY_SUPPORT, EXTRA);
    }

    /** 긴급복지 금융재산 상한 (원). */
    public long emergencyAssetCap() {
        return required(EMERGENCY_ASSET_CAP, "CAP");
    }

    /** 실업급여 1일 구직급여 하한 (원). */
    public long uiDailyMin() {
        return required(UI_DAILY, "MIN");
    }

    /** 실업급여 1일 구직급여 상한 (원). */
    public long uiDailyMax() {
        return required(UI_DAILY, "MAX");
    }

    /** 본인부담상한 (연, 원). 월 소득이 구간 상한 이하인 첫 행의 상한을 반환. */
    public long outOfPocketCap(long monthlyIncome) {
        for (long[] tier : oopCapTiers) {
            if (monthlyIncome <= tier[0]) {
                return tier[1];
            }
        }
        throw new IllegalStateException(
                "No OOP_CAP tier found for monthly income " + monthlyIncome
                        + " — benefit_criteria seed is missing the top income tier.");
    }

    /** 장기요양 등급별 재가급여 월 한도 (원). 등급 미정의 시 0. */
    public long careLimit(int grade) {
        Map<String, Long> rows = byCategory.get(CARE_LIMIT);
        if (rows == null) {
            return 0L;
        }
        return rows.getOrDefault(String.valueOf(grade), 0L);
    }

    private long required(String category, String bracketKey) {
        Map<String, Long> rows = byCategory.get(category);
        Long value = (rows != null) ? rows.get(bracketKey) : null;
        if (value == null) {
            throw new IllegalStateException(
                    "Missing benefit_criteria row for category=" + category
                            + ", bracketKey=" + bracketKey);
        }
        return value;
    }
}
