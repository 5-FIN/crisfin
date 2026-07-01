package com.finfive.crisfin.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request body of {@code POST /api/v1/payments/checkout}.
 *
 * <p>{@code plan}은 요금제 코드({@code PaymentPlan.name()}, 예: {@code SINGLE}, {@code UNLIMITED_30D}).</p>
 */
@Getter
@NoArgsConstructor
public class CheckoutRequest {

    @NotBlank(message = "plan은 필수입니다.")
    private String plan;
}
