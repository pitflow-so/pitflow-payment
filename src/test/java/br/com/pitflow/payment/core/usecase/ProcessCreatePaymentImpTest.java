package br.com.pitflow.payment.core.usecase;

import br.com.pitflow.common.core.gateway.ClockGateway;
import br.com.pitflow.common.core.gateway.TransactionGateway;
import br.com.pitflow.payment.core.entity.Payment;
import br.com.pitflow.payment.core.entity.PaymentAttempt;
import br.com.pitflow.payment.core.gateway.PaymentAttemptGateway;
import br.com.pitflow.payment.core.gateway.PaymentGateway;
import br.com.pitflow.payment.core.gateway.PaymentLinkEventGateway;
import br.com.pitflow.payment.core.gateway.PaymentProviderGateway;
import br.com.pitflow.payment.core.usecase.inputPort.CreatePayment;
import br.com.pitflow.payment.core.usecase.inputPort.ProcessCreatePayment;
import br.com.pitflow.payment.core.usecase.outputData.PaymentOutput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProcessCreatePaymentImpTest {
    private final CreatePayment create = mock(CreatePayment.class);
    private final PaymentGateway payments = mock(PaymentGateway.class);
    private final PaymentAttemptGateway attempts = mock(PaymentAttemptGateway.class);
    private final PaymentProviderGateway provider = mock(PaymentProviderGateway.class);
    private final PaymentLinkEventGateway events = mock(PaymentLinkEventGateway.class);
    private final TransactionGateway tx = new TransactionGateway() {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    };
    private final Instant now = Instant.parse("2026-07-27T00:00:00Z");
    private final ClockGateway clock = () -> now;

    @Test
    void reusesExistingLocalAttemptWithoutCallingProvider() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        when(create.execute(any())).thenReturn(new PaymentOutput(paymentId, orderId, 1, BigDecimal.TEN, "BRL",
                br.com.pitflow.payment.core.enums.PaymentStatus.CHECKOUT_PENDING));
        when(attempts.findFirstByPaymentId(paymentId)).thenReturn(Optional.of(new PaymentAttempt(UUID.randomUUID(),
                paymentId, "pref-1", null, "https://sandbox.mercadopago.com/checkout", "PREFERENCE_CREATED",
                null, now.plusSeconds(3600), now, now)));

        var result = useCase().execute(command(orderId));

        assertThat(result.alreadyProcessed()).isTrue();
        verifyNoInteractions(provider, events);
    }

    @Test
    void searchesBeforeCreatingPreferenceAndPersistsEventAtomically() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Payment payment = Payment.create(paymentId, orderId, 1, "payment:" + paymentId, UUID.randomUUID().toString(),
                "hash", BigDecimal.TEN, "BRL", null, now);
        when(create.execute(any())).thenReturn(PaymentOutput.from(payment));
        when(payments.findById(paymentId)).thenReturn(Optional.of(payment));
        when(attempts.findFirstByPaymentId(paymentId)).thenReturn(Optional.empty());
        when(provider.findCheckoutPreference(payment.getExternalReference())).thenReturn(Optional.empty());
        when(provider.createCheckoutPreference(any())).thenReturn(
                new PaymentProviderGateway.CheckoutPreferenceResult("pref-2", "https://sandbox.mercadopago.com/pay",
                        now.plusSeconds(86400)));
        when(attempts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(payments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase().execute(command(orderId));

        assertThat(result.checkoutUrl()).contains("mercadopago.com");
        assertThat(payment.getStatus()).isEqualTo(br.com.pitflow.payment.core.enums.PaymentStatus.CHECKOUT_PENDING);
        verify(provider).findCheckoutPreference(payment.getExternalReference());
        verify(provider).createCheckoutPreference(any());
        verify(events).save(any());
    }

    private ProcessCreatePaymentImp useCase() {
        return new ProcessCreatePaymentImp(create, payments, attempts, provider, events, tx, clock,
                "https://api.example/payment/webhooks/mercado-pago");
    }

    private ProcessCreatePayment.Command command(UUID orderId) {
        UUID sagaId = UUID.randomUUID();
        return new ProcessCreatePayment.Command(UUID.randomUUID(), sagaId, UUID.randomUUID(), orderId,
                BigDecimal.TEN, "BRL", "OS " + orderId, sagaId.toString());
    }
}
