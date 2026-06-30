package com.finfive.crisfin.domain.payment;

import com.finfive.crisfin.domain.payment.dto.CheckoutResponse;
import com.finfive.crisfin.domain.payment.dto.ConfirmRequest;
import com.finfive.crisfin.domain.payment.dto.EntitlementResponse;
import com.finfive.crisfin.domain.user.User;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import com.finfive.crisfin.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Payments / entitlement endpoints. All require authentication.
 *
 * <ul>
 *   <li>{@code POST /api/v1/payments/checkout}    – create a pending order.</li>
 *   <li>{@code POST /api/v1/payments/confirm}     – settle order, activate entitlement.</li>
 *   <li>{@code GET  /api/v1/payments/entitlement} – current entitlement status.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = requireUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.ok(paymentService.checkout(userId)));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<EntitlementResponse>> confirm(
            @Valid @RequestBody ConfirmRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = requireUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.ok(paymentService.confirm(userId, request.getOrderUid())));
    }

    @GetMapping("/entitlement")
    public ResponseEntity<ApiResponse<EntitlementResponse>> entitlement(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = requireUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getEntitlement(userId)));
    }

    private Long requireUserId(UserDetails userDetails) {
        if (userDetails instanceof User user) {
            return user.getId();
        }
        throw new CrisfinException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
    }
}
