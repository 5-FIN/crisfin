package com.finfive.crisfin.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Structured applicant inputs used by the benefit rule engine to compute eligibility and
 * estimated amounts. All fields are optional; missing values surface as
 * {@code NEEDS_MORE_INPUT} for benefits that require them.
 *
 * <p>{@code monthlyIncome}, {@code liquidFinancialAssets} and {@code annualOutOfPocketMedical}
 * are expressed in KRW (원).</p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicantProfile {

    private Integer householdSize;
    private Long monthlyIncome;
    private Integer age;
    private Long liquidFinancialAssets;
    private Integer employmentInsuranceMonths;
    private Boolean involuntarySeparation;
    private Long annualOutOfPocketMedical;
    private Integer careGrade;
}
