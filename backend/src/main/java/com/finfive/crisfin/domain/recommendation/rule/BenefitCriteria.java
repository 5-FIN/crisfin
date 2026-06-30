package com.finfive.crisfin.domain.recommendation.rule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

/**
 * A single rule-engine threshold row, data-ised from the previously hardcoded
 * {@code BenefitRuleEngine} constants. Identified by {@code (category, bracketKey)}.
 *
 * @see BenefitCriteriaProvider
 */
@Entity
@Table(name = "benefit_criteria")
@Getter
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor
@Builder
public class BenefitCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String category;

    @Column(name = "bracket_key")
    private String bracketKey;

    private long amount;

    private String note;
}
