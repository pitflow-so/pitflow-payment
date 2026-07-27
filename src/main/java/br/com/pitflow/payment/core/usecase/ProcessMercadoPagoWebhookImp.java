package br.com.pitflow.payment.core.usecase;

import br.com.pitflow.common.core.gateway.ClockGateway;
import br.com.pitflow.common.core.gateway.TransactionGateway;
import br.com.pitflow.payment.core.enums.PaymentStatus;
import br.com.pitflow.payment.core.gateway.*;
import br.com.pitflow.payment.core.usecase.inputPort.ProcessMercadoPagoWebhook;

import java.util.UUID;

public final class ProcessMercadoPagoWebhookImp implements ProcessMercadoPagoWebhook {
    private final WebhookEventGateway webhooks;
    private final PaymentProviderGateway provider;
    private final PaymentGateway payments;
    private final PaymentAttemptGateway attempts;
    private final PaymentStatusEventGateway events;
    private final TransactionGateway tx;
    private final ClockGateway clock;

    public ProcessMercadoPagoWebhookImp(WebhookEventGateway webhooks, PaymentProviderGateway provider,
                                        PaymentGateway payments, PaymentAttemptGateway attempts,
                                        PaymentStatusEventGateway events, TransactionGateway tx, ClockGateway clock) {
        this.webhooks = webhooks;
        this.provider = provider;
        this.payments = payments;
        this.attempts = attempts;
        this.events = events;
        this.tx = tx;
        this.clock = clock;
    }

    @Override
    public Result execute(Command command) {
        validate(command);
        if (webhooks.existsByEventKey(command.eventKey())) return new Result(Status.DUPLICATE, null);

        var official = provider.findPaymentByProviderId(command.paymentId());
        var payment = payments.findByExternalReference(official.externalReference()).orElse(null);
        if (payment == null) return new Result(Status.IGNORED, null);
        if (official.amount() == null || payment.getAmount().compareTo(official.amount()) != 0
                || !payment.getCurrency().equals(official.currency())) {
            throw new IllegalArgumentException("Provider payment amount or currency does not match");
        }

        return tx.execute(() -> {
            if (webhooks.existsByEventKey(command.eventKey())) return new Result(Status.DUPLICATE, payment.getId());
            var now = clock.now();
            attempts.findFirstByPaymentId(payment.getId()).ifPresent(attempt ->
                    attempts.save(attempt.withProviderPayment(official.providerPaymentId(), official.status(),
                            official.statusDetail(), now)));

            var previous = payment.getStatus();
            var result = applyStatus(payment, official.status(), official.approvedAt() == null ? now : official.approvedAt());
            if (payment.getStatus() != previous) payments.save(payment);

            webhooks.save(new WebhookEventGateway.WebhookEvent(command.eventKey(), command.notificationId(),
                    official.providerPaymentId(), command.action(), command.rawPayload(), now, now));

            if (payment.getStatus() != previous) publish(payment, official, now);
            return new Result(result, payment.getId());
        });
    }

    private Status applyStatus(br.com.pitflow.payment.core.entity.Payment payment, String status, java.time.Instant at) {
        return switch (status) {
            case "approved" -> {
                if (payment.getStatus() != PaymentStatus.APPROVED) payment.approve(at);
                yield Status.PROCESSED;
            }
            case "rejected" -> {
                if (payment.getStatus() != PaymentStatus.REJECTED) payment.reject(at);
                yield Status.PROCESSED;
            }
            case "pending" -> {
                if (payment.getStatus() != PaymentStatus.PENDING) payment.markPending(at);
                yield Status.PROCESSED;
            }
            case "in_process", "in_mediation" -> {
                if (payment.getStatus() != PaymentStatus.IN_PROCESS) payment.markInProcess(at);
                yield Status.PROCESSED;
            }
            default -> Status.IGNORED;
        };
    }

    private void publish(br.com.pitflow.payment.core.entity.Payment payment,
                         PaymentProviderGateway.ProviderPaymentResult official, java.time.Instant now) {
        UUID sagaId = UUID.fromString(payment.getIdempotencyKey());
        if (payment.getStatus() == PaymentStatus.APPROVED) {
            events.approved(new PaymentStatusEventGateway.Approved(UUID.randomUUID(), sagaId, sagaId,
                    payment.getServiceOrderId(), payment.getId(), official.providerPaymentId(), payment.getAmount(),
                    payment.getCurrency(), now));
        } else if (payment.getStatus() == PaymentStatus.REJECTED) {
            events.rejected(new PaymentStatusEventGateway.Rejected(UUID.randomUUID(), sagaId, sagaId,
                    payment.getServiceOrderId(), payment.getId(),
                    official.statusDetail() == null ? "rejected" : official.statusDetail(),
                    "Pagamento rejeitado pelo Mercado Pago", now));
        }
    }

    private static void validate(Command command) {
        if (blank(command.eventKey()) || blank(command.notificationId()) || blank(command.paymentId())
                || blank(command.action()) || blank(command.rawPayload())) {
            throw new IllegalArgumentException("Invalid Mercado Pago webhook");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
