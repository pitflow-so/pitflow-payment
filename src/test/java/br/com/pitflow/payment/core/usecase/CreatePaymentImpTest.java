package br.com.pitflow.payment.core.usecase;

import br.com.pitflow.common.core.gateway.TransactionGateway;
import br.com.pitflow.payment.core.entity.Payment;
import br.com.pitflow.payment.core.exception.DuplicatePaymentException;
import br.com.pitflow.payment.core.exception.PaymentIdempotencyConflictException;
import br.com.pitflow.payment.core.gateway.PaymentGateway;
import br.com.pitflow.payment.core.usecase.inputPort.CreatePayment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreatePaymentImpTest {
    PaymentGateway gateway = mock(PaymentGateway.class);
    CreatePaymentImp usecase = new CreatePaymentImp(gateway, new TransactionGateway() {
        public <T> T execute(java.util.function.Supplier<T> s) {
            return s.get();
        }
    }, () -> Instant.EPOCH, value -> "hash");
    UUID order = UUID.randomUUID();

    CreatePayment.Command command() {
        return new CreatePayment.Command(order, 1, new BigDecimal("10.00"), "BRL", "payer@example.com", "key");
    }

    @Test
    void createsNewPayment() {
        when(gateway.findByIdempotencyKey("key")).thenReturn(Optional.empty());
        when(gateway.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(usecase.execute(command()).status().name()).isEqualTo("CREATED");
        verify(gateway).save(any());
    }

    @Test
    void returnsExistingForSameKeyAndPayload() {
        Payment p = Payment.create(UUID.randomUUID(), order, 1, "ext", "key", "hash", new BigDecimal("10.00"), "BRL", "payer@example.com", Instant.EPOCH);
        when(gateway.findByIdempotencyKey("key")).thenReturn(Optional.of(p));
        assertThat(usecase.execute(command()).id()).isEqualTo(p.getId());
        verify(gateway, never()).save(any());
    }

    @Test
    void conflictsForDifferentPayload() {
        Payment p = Payment.create(UUID.randomUUID(), order, 1, "ext", "key", "other", BigDecimal.ONE, "BRL", "payer@example.com", Instant.EPOCH);
        when(gateway.findByIdempotencyKey("key")).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> usecase.execute(command())).isInstanceOf(PaymentIdempotencyConflictException.class);
    }

    @Test
    void rejectsDuplicateOrderVersion() {
        when(gateway.findByIdempotencyKey("key")).thenReturn(Optional.empty());
        when(gateway.existsByServiceOrderIdAndBudgetVersion(order, 1)).thenReturn(true);
        assertThatThrownBy(() -> usecase.execute(command())).isInstanceOf(DuplicatePaymentException.class);
    }
}
