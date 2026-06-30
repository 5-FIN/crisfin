package com.finfive.crisfin.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request body of {@code POST /api/v1/payments/confirm}.
 */
@Getter
@NoArgsConstructor
public class ConfirmRequest {

    @NotBlank(message = "orderUid는 필수입니다.")
    private String orderUid;
}
