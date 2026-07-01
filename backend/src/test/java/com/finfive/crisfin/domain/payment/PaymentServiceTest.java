package com.finfive.crisfin.domain.payment;

import com.finfive.crisfin.domain.payment.dto.CheckoutResponse;
import com.finfive.crisfin.domain.payment.dto.EntitlementResponse;
import com.finfive.crisfin.global.exception.CrisfinException;
import com.finfive.crisfin.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentOrderRepository paymentOrderRepository;

    @Mock
    private EntitlementRepository entitlementRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void checkout_createsPendingOrder_withPlanAmount() {
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckoutResponse res = paymentService.checkout(1L, PaymentPlan.SINGLE.name());

        assertThat(res.getAmount()).isEqualTo(9900);
        assertThat(res.getPlan()).isEqualTo("SINGLE");
        assertThat(res.getOrderUid()).startsWith("order_");

        ArgumentCaptor<PaymentOrder> captor = ArgumentCaptor.forClass(PaymentOrder.class);
        org.mockito.Mockito.verify(paymentOrderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentOrder.OrderStatus.PENDING);
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getPlan()).isEqualTo("SINGLE");
    }

    @Test
    void checkout_unknownPlan_throwsPlanNotFound() {
        assertThatThrownBy(() -> paymentService.checkout(1L, "BOGUS"))
                .isInstanceOf(CrisfinException.class)
                .extracting(e -> ((CrisfinException) e).getErrorCode())
                .isEqualTo(ErrorCode.PLAN_NOT_FOUND);
    }

    @Test
    void confirm_singlePlan_activatesWithOneUse_andExpiry() {
        PaymentOrder order = PaymentOrder.builder()
                .orderUid("order_abc")
                .userId(1L)
                .amount(9900)
                .status(PaymentOrder.OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .plan(PaymentPlan.SINGLE.name())
                .build();
        when(paymentOrderRepository.findByOrderUid("order_abc")).thenReturn(Optional.of(order));
        when(entitlementRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(entitlementRepository.save(any(Entitlement.class))).thenAnswer(inv -> inv.getArgument(0));

        EntitlementResponse res = paymentService.confirm(1L, "order_abc");

        assertThat(res.isActive()).isTrue();
        assertThat(res.getPlan()).isEqualTo("SINGLE");
        assertThat(res.getRemainingUses()).isEqualTo(1);
        assertThat(res.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(29));
        assertThat(order.isPaid()).isTrue();
    }

    @Test
    void confirm_unlimitedPlan_activatesUnlimited_withExpiry() {
        PaymentOrder order = PaymentOrder.builder()
                .orderUid("order_unl")
                .userId(1L)
                .amount(19900)
                .status(PaymentOrder.OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .plan(PaymentPlan.UNLIMITED_30D.name())
                .build();
        when(paymentOrderRepository.findByOrderUid("order_unl")).thenReturn(Optional.of(order));
        when(entitlementRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(entitlementRepository.save(any(Entitlement.class))).thenAnswer(inv -> inv.getArgument(0));

        EntitlementResponse res = paymentService.confirm(1L, "order_unl");

        assertThat(res.isActive()).isTrue();
        assertThat(res.getPlan()).isEqualTo("UNLIMITED_30D");
        assertThat(res.getRemainingUses()).isNull();
        assertThat(res.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(29));
    }

    @Test
    void consumeUse_singlePlan_decrementsToZero_thenInactive() {
        Entitlement single = Entitlement.builder()
                .userId(1L)
                .plan(PaymentPlan.SINGLE.name())
                .status(Entitlement.EntitlementStatus.ACTIVE)
                .activatedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .remainingUses(1)
                .build();
        when(entitlementRepository.findByUserId(1L)).thenReturn(Optional.of(single));

        assertThat(single.isActive()).isTrue();

        paymentService.consumeUse(1L);

        assertThat(single.getRemainingUses()).isZero();
        assertThat(single.isActive()).isFalse();
    }

    @Test
    void consumeUse_unlimitedPlan_unaffected() {
        Entitlement unlimited = Entitlement.builder()
                .userId(1L)
                .plan(PaymentPlan.UNLIMITED_30D.name())
                .status(Entitlement.EntitlementStatus.ACTIVE)
                .activatedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .remainingUses(null)
                .build();
        when(entitlementRepository.findByUserId(1L)).thenReturn(Optional.of(unlimited));

        paymentService.consumeUse(1L);

        assertThat(unlimited.getRemainingUses()).isNull();
        assertThat(unlimited.isActive()).isTrue();
    }

    @Test
    void hasActiveEntitlement_reflectsStoredEntitlement() {
        Entitlement active = Entitlement.builder()
                .userId(1L)
                .plan(PaymentPlan.SINGLE.name())
                .status(Entitlement.EntitlementStatus.ACTIVE)
                .activatedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .remainingUses(1)
                .build();
        when(entitlementRepository.findByUserId(1L)).thenReturn(Optional.of(active));
        when(entitlementRepository.findByUserId(2L)).thenReturn(Optional.empty());

        assertThat(paymentService.hasActiveEntitlement(1L)).isTrue();
        assertThat(paymentService.hasActiveEntitlement(2L)).isFalse();
    }
}
