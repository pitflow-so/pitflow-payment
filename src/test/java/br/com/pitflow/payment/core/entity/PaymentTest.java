package br.com.pitflow.payment.core.entity;

import br.com.pitflow.payment.core.enums.PaymentStatus;
import br.com.pitflow.payment.core.exception.InvalidPaymentDataException;
import br.com.pitflow.payment.core.exception.InvalidPaymentStatusTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {
    private final Instant now = Instant.parse("2026-07-14T12:00:00Z");

    private Payment valid() {
        return Payment.create(UUID.randomUUID(), UUID.randomUUID(), 1, "ext", "key", "hash", new BigDecimal("10.00"), "BRL", "payer@example.com", now);
    }

    @Test
    void createsValidPayment() {
        assertThat(valid().getStatus()).isEqualTo(PaymentStatus.CREATED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void rejectsInvalidAmounts(String value) {
        assertThatThrownBy(() -> Payment.create(UUID.randomUUID(), UUID.randomUUID(), 1, "ext", "key", "hash", new BigDecimal(value), "BRL", "payer@example.com", now)).isInstanceOf(InvalidPaymentDataException.class);
    }

    @Test
    void rejectsUnsupportedCurrency() {
        assertThatThrownBy(() -> Payment.create(UUID.randomUUID(), UUID.randomUUID(), 1, "ext", "key", "hash", BigDecimal.ONE, "USD", "payer@example.com", now)).isInstanceOf(InvalidPaymentDataException.class);
    }

    @Test
    void rejectsRequiredFields() {
        assertThatThrownBy(() -> Payment.create(null, UUID.randomUUID(), 1, "ext", "key", "hash", BigDecimal.ONE, "BRL", "payer@example.com", now)).isInstanceOf(InvalidPaymentDataException.class);
    }

    @Test
    void supportsValidLifecycleTransitions() {
        Payment p = valid();
        p.markCheckoutPending(now.plusSeconds(1));
        p.markPending(now.plusSeconds(2));
        p.markInProcess(now.plusSeconds(3));
        p.approve(now.plusSeconds(4));
        p.refund(now.plusSeconds(5));
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void supportsRejectionCancellationAndExpiration() {
        Payment rejected = valid();
        rejected.markCheckoutPending(now);
        rejected.reject(now);
        Payment cancelled = valid();
        cancelled.cancel(now);
        Payment expired = valid();
        expired.markCheckoutPending(now);
        expired.expire(now);
        assertThat(rejected.getStatus()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(cancelled.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(expired.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
    }

    @Test
    void rejectsInvalidTransitionAndTerminalRegression() {
        Payment p = valid();
        assertThatThrownBy(() -> p.approve(now)).isInstanceOf(InvalidPaymentStatusTransitionException.class);
        p.cancel(now);
        assertThatThrownBy(() -> p.markPending(now)).isInstanceOf(InvalidPaymentStatusTransitionException.class);
    }
}
