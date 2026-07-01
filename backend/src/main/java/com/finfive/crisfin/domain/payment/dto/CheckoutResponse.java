package com.finfive.crisfin.domain.payment.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Response of {@code POST /api/v1/payments/checkout}.
 *
 * @param orderUid opaque order identifier to pass back to {@code confirm}
 * @param amount   price to be paid (KRW)
 * @param plan     구매한 요금제 코드({@code PaymentPlan.name()})
 */
@Getter
@Builder
public class CheckoutResponse {
    private final String orderUid;
    private final int amount;
    private final String plan;
}
