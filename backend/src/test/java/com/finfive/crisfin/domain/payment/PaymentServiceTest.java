package com.finfive.crisfin.domain.payment;

import com.finfive.crisfin.domain.payment.dto.CheckoutResponse;
import com.finfive.crisfin.domain.payment.dto.EntitlementResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "price", 9900);
        ReflectionTestUtils.setField(paymentService, "plan", "STANDARD");
    }

    @Test
    void checkout_createsPendingOrder_withConfiguredAmount() {
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckoutResponse res = paymentService.checkout(1L);

        assertThat(res.getAmount()).isEqualTo(9900);
        assertThat(res.getOrderUid()).startsWith("order_");

        ArgumentCaptor<PaymentOrder> captor = ArgumentCaptor.forClass(PaymentOrder.class);
        org.mockito.Mockito.verify(paymentOrderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentOrder.OrderStatus.PENDING);
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
    }

    @Test
    void confirm_activatesEntitlement_andMarksOrderPaid() {
        PaymentOrder order = PaymentOrder.builder()
                .orderUid("order_abc")
                .userId(1L)
                .amount(9900)
                .status(PaymentOrder.OrderStatus.PENDING)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        when(paymentOrderRepository.findByOrderUid("order_abc")).thenReturn(Optional.of(order));
        when(entitlementRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(entitlementRepository.save(any(Entitlement.class))).thenAnswer(inv -> inv.getArgument(0));

        EntitlementResponse res = paymentService.confirm(1L, "order_abc");

        assertThat(res.isActive()).isTrue();
        assertThat(res.getPlan()).isEqualTo("STANDARD");
        assertThat(order.isPaid()).isTrue();
    }

    @Test
    void hasActiveEntitlement_reflectsStoredEntitlement() {
        Entitlement active = Entitlement.builder()
                .userId(1L)
                .plan("STANDARD")
                .status(Entitlement.EntitlementStatus.ACTIVE)
                .activatedAt(java.time.LocalDateTime.now())
                .expiresAt(null)
                .build();
        when(entitlementRepository.findByUserId(1L)).thenReturn(Optional.of(active));
        when(entitlementRepository.findByUserId(2L)).thenReturn(Optional.empty());

        assertThat(paymentService.hasActiveEntitlement(1L)).isTrue();
        assertThat(paymentService.hasActiveEntitlement(2L)).isFalse();
    }
}
