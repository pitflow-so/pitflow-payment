package br.com.pitflow.payment.core.usecase;

import br.com.pitflow.common.core.gateway.ClockGateway;
import br.com.pitflow.common.core.gateway.TransactionGateway;
import br.com.pitflow.payment.core.entity.Payment;
import br.com.pitflow.payment.core.enums.PaymentStatus;
import br.com.pitflow.payment.core.gateway.PaymentGateway;
import br.com.pitflow.payment.core.gateway.PaymentStatusEventGateway;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RejectPaymentForHomologationImpTest {
    private final PaymentGateway payments = mock(PaymentGateway.class);
    private final PaymentStatusEventGateway events = mock(PaymentStatusEventGateway.class);
    private final Instant now = Instant.parse("2026-07-27T09:00:00Z");
    private final ClockGateway clock = () -> now;
    private final TransactionGateway tx = new TransactionGateway() {
        public <T> T execute(Supplier<T> operation) { return operation.get(); }
    };

    @Test
    void rejectsEligiblePaymentAndPublishesEvent() {
        UUID orderId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();
        Payment payment = Payment.create(UUID.randomUUID(), orderId, 1, "payment:test",
                sagaId.toString(), "hash", new BigDecimal("250.99"), "BRL", null, now.minusSeconds(30));
        payment.markCheckoutPending(now.minusSeconds(20));
        when(payments.findByServiceOrderId(orderId)).thenReturn(List.of(payment));

        var result = new RejectPaymentForHomologationImp(payments, events, tx, clock).execute(orderId);

        assertThat(result.status()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(result.alreadyRejected()).isFalse();
        verify(payments).save(payment);
        verify(events).rejected(argThat(e -> e.sagaId().equals(sagaId)
                && e.serviceOrderId().equals(orderId)));
    }

    @Test
    void replayIsIdempotent() {
        UUID orderId = UUID.randomUUID();
        Payment payment = Payment.create(UUID.randomUUID(), orderId, 1, "payment:test",
                UUID.randomUUID().toString(), "hash", BigDecimal.ONE, "BRL", null, now.minusSeconds(30));
        payment.markCheckoutPending(now.minusSeconds(20));
        payment.reject(now.minusSeconds(10));
        when(payments.findByServiceOrderId(orderId)).thenReturn(List.of(payment));

        var result = new RejectPaymentForHomologationImp(payments, events, tx, clock).execute(orderId);

        assertThat(result.alreadyRejected()).isTrue();
        verify(payments, never()).save(any());
        verifyNoInteractions(events);
    }
}
