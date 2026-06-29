package com.finfive.crisfin.domain.payment.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Response of {@code POST /api/v1/payments/checkout}.
 *
 * @param orderUid opaque order identifier to pass back to {@code confirm}
 * @param amount   price to be paid (KRW)
 */
@Getter
@Builder
public class CheckoutResponse {
    private final String orderUid;
    private final int amount;
}
