package br.com.pitflow.payment.core.usecase;

import br.com.pitflow.common.core.gateway.ClockGateway;
import br.com.pitflow.common.core.gateway.TransactionGateway;
import br.com.pitflow.payment.core.entity.Payment;
import br.com.pitflow.payment.core.entity.PaymentAttempt;
import br.com.pitflow.payment.core.enums.PaymentStatus;
import br.com.pitflow.payment.core.gateway.*;
import br.com.pitflow.payment.core.usecase.inputPort.ProcessMercadoPagoWebhook;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProcessMercadoPagoWebhookImpTest {
    private final WebhookEventGateway webhooks = mock(WebhookEventGateway.class);
    private final PaymentProviderGateway provider = mock(PaymentProviderGateway.class);
    private final PaymentGateway payments = mock(PaymentGateway.class);
    private final PaymentAttemptGateway attempts = mock(PaymentAttemptGateway.class);
    private final PaymentStatusEventGateway events = mock(PaymentStatusEventGateway.class);
    private final Instant now = Instant.parse("2026-07-27T01:00:00Z");
    private final ClockGateway clock = () -> now;
    private final TransactionGateway tx = new TransactionGateway() {
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    };

    @Test
    void approvesFromOfficialProviderDataAndPublishesOnce() {
        UUID sagaId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Payment payment = Payment.create(UUID.randomUUID(), orderId, 1, "payment:123", sagaId.toString(), "hash",
                new BigDecimal("450.00"), "BRL", null, now.minusSeconds(60));
        payment.markCheckoutPending(now.minusSeconds(50));
        PaymentAttempt attempt = new PaymentAttempt(UUID.randomUUID(), payment.getId(), "pref", null,
                "https://checkout", "PREFERENCE_CREATED", null, now.plusSeconds(3600), now.minusSeconds(50),
                now.minusSeconds(50));
        when(provider.findPaymentByProviderId("987")).thenReturn(new PaymentProviderGateway.ProviderPaymentResult(
                "987", "approved", "accredited", "payment:123", new BigDecimal("450.00"), "BRL", now));
        when(payments.findByExternalReference("payment:123")).thenReturn(Optional.of(payment));
        when(attempts.findFirstByPaymentId(payment.getId())).thenReturn(Optional.of(attempt));

        var result = useCase().execute(command());

        assertThat(result.status()).isEqualTo(ProcessMercadoPagoWebhook.Status.PROCESSED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        verify(payments).save(payment);
        verify(attempts).save(argThat(value -> "987".equals(value.providerPaymentId())));
        verify(webhooks).save(any());
        verify(events).approved(any());
        verify(events, never()).rejected(any());
    }

    @Test
    void duplicateDoesNotConsultProviderOrRepeatEffects() {
        when(webhooks.existsByEventKey("notification:updated:987")).thenReturn(true);

        var result = useCase().execute(command());

        assertThat(result.status()).isEqualTo(ProcessMercadoPagoWebhook.Status.DUPLICATE);
        verifyNoInteractions(provider, payments, attempts, events);
    }

    @Test
    void ignoresProviderPaymentThatDoesNotBelongToPitflow() {
        when(provider.findPaymentByProviderId("987")).thenReturn(new PaymentProviderGateway.ProviderPaymentResult(
                "987", "approved", "accredited", "OS-TESTE-001", new BigDecimal("10.00"), "BRL", now));
        when(payments.findByExternalReference("OS-TESTE-001")).thenReturn(Optional.empty());

        var result = useCase().execute(command());

        assertThat(result.status()).isEqualTo(ProcessMercadoPagoWebhook.Status.IGNORED);
        assertThat(result.localPaymentId()).isNull();
        verifyNoInteractions(attempts, events);
        verify(webhooks, never()).save(any());
    }

    @Test
    void rejectsTamperedAmountBeforeChangingLocalState() {
        Payment payment = Payment.create(UUID.randomUUID(), UUID.randomUUID(), 1, "payment:123",
                UUID.randomUUID().toString(), "hash", new BigDecimal("450.00"), "BRL", null, now);
        when(provider.findPaymentByProviderId("987")).thenReturn(new PaymentProviderGateway.ProviderPaymentResult(
                "987", "approved", "accredited", "payment:123", new BigDecimal("1.00"), "BRL", now));
        when(payments.findByExternalReference("payment:123")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> useCase().execute(command()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");
        verify(events, never()).approved(any());
        verify(webhooks, never()).save(any());
    }

    private ProcessMercadoPagoWebhookImp useCase() {
        return new ProcessMercadoPagoWebhookImp(webhooks, provider, payments, attempts, events, tx, clock);
    }

    private ProcessMercadoPagoWebhook.Command command() {
        return new ProcessMercadoPagoWebhook.Command("notification:updated:987", "notification", "987",
                "payment.updated", "{\"type\":\"payment\"}");
    }
}
