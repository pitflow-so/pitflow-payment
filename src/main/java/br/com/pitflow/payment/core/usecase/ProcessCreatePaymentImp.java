package br.com.pitflow.payment.core.usecase;

import br.com.pitflow.common.core.gateway.ClockGateway;
import br.com.pitflow.common.core.gateway.TransactionGateway;
import br.com.pitflow.payment.core.entity.PaymentAttempt;
import br.com.pitflow.payment.core.gateway.PaymentAttemptGateway;
import br.com.pitflow.payment.core.gateway.PaymentGateway;
import br.com.pitflow.payment.core.gateway.PaymentLinkEventGateway;
import br.com.pitflow.payment.core.gateway.PaymentProviderGateway;
import br.com.pitflow.payment.core.usecase.inputPort.CreatePayment;
import br.com.pitflow.payment.core.usecase.inputPort.ProcessCreatePayment;

import java.util.UUID;

public final class ProcessCreatePaymentImp implements ProcessCreatePayment {
    private final CreatePayment createPayment;
    private final PaymentGateway payments;
    private final PaymentAttemptGateway attempts;
    private final PaymentProviderGateway provider;
    private final PaymentLinkEventGateway events;
    private final TransactionGateway tx;
    private final ClockGateway clock;

    public ProcessCreatePaymentImp(CreatePayment createPayment, PaymentGateway payments, PaymentAttemptGateway attempts,
                                   PaymentProviderGateway provider, PaymentLinkEventGateway events,
                                   TransactionGateway tx, ClockGateway clock) {
        this.createPayment = createPayment;
        this.payments = payments;
        this.attempts = attempts;
        this.provider = provider;
        this.events = events;
        this.tx = tx;
        this.clock = clock;
    }

    @Override
    public Result execute(Command command) {
        validate(command);
        var created = createPayment.execute(new CreatePayment.Command(
                command.serviceOrderId(), 1, command.amount(), command.currency(), null, command.idempotencyKey()));
        var previous = attempts.findFirstByPaymentId(created.id());
        if (previous.isPresent()) {
            var attempt = previous.get();
            return new Result(created.id(), attempt.providerPreferenceId(), attempt.checkoutUrl(), true);
        }

        var payment = payments.findById(created.id()).orElseThrow();
        var requestedExpiration = clock.now().plusSeconds(24 * 60 * 60);
        var preference = provider.findCheckoutPreference(payment.getExternalReference())
                .orElseGet(() -> provider.createCheckoutPreference(new PaymentProviderGateway.CheckoutPreferenceCommand(
                        payment.getExternalReference(), command.description(), command.amount(), command.currency(),
                        requestedExpiration)));

        return tx.execute(() -> {
            var concurrent = attempts.findFirstByPaymentId(payment.getId());
            if (concurrent.isPresent()) {
                var attempt = concurrent.get();
                return new Result(payment.getId(), attempt.providerPreferenceId(), attempt.checkoutUrl(), true);
            }
            var now = clock.now();
            attempts.save(new PaymentAttempt(UUID.randomUUID(), payment.getId(), preference.preferenceId(), null,
                    preference.checkoutUrl(), "PREFERENCE_CREATED", null, preference.expiresAt(), now, now));
            payment.markCheckoutPending(now);
            payments.save(payment);
            events.save(new PaymentLinkEventGateway.Event(UUID.randomUUID(), command.sagaId(), command.correlationId(),
                    command.serviceOrderId(), payment.getId(), preference.preferenceId(), preference.checkoutUrl(),
                    payment.getAmount(), payment.getCurrency(),
                    preference.expiresAt() == null ? requestedExpiration : preference.expiresAt(), now));
            return new Result(payment.getId(), preference.preferenceId(), preference.checkoutUrl(), false);
        });
    }

    private static void validate(Command command) {
        if (command.messageId() == null || command.sagaId() == null || command.correlationId() == null
                || command.serviceOrderId() == null || command.amount() == null || command.amount().signum() <= 0
                || !"BRL".equals(command.currency()) || command.idempotencyKey() == null
                || command.idempotencyKey().isBlank()) {
            throw new IllegalArgumentException("Invalid CreatePayment command");
        }
    }
}
