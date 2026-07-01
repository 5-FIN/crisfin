package com.finfive.crisfin.domain.payment.dto;

import com.finfive.crisfin.domain.payment.PaymentPlan;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 요금제 카탈로그 항목. {@code GET /api/v1/payments/plans}의 응답 요소.
 *
 * @param code         요금제 코드({@code PaymentPlan.name()})
 * @param name         노출 이름
 * @param priceKrw     가격 (원)
 * @param durationDays 유효기간 (일)
 * @param uses         제공 이용 횟수 ({@code null}이면 무제한)
 * @param tagline      마케팅 문구
 * @param features     기능 목록
 */
@Getter
@Builder
public class PlanResponse {

    private final String code;
    private final String name;
    private final int priceKrw;
    private final int durationDays;
    private final Integer uses;
    private final String tagline;
    private final List<String> features;

    /** 요금제 enum을 응답 DTO로 매핑한다. */
    public static PlanResponse from(PaymentPlan plan) {
        return PlanResponse.builder()
                .code(plan.name())
                .name(plan.getDisplayName())
                .priceKrw(plan.getPriceKrw())
                .durationDays(plan.getDurationDays())
                .uses(plan.getUses())
                .tagline(plan.getTagline())
                .features(plan.getFeatures())
                .build();
    }
}
