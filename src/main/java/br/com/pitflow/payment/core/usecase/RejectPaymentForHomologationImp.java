package br.com.pitflow.payment.core.usecase;

import br.com.pitflow.common.core.gateway.ClockGateway;
import br.com.pitflow.common.core.gateway.TransactionGateway;
import br.com.pitflow.payment.core.entity.Payment;
import br.com.pitflow.payment.core.enums.PaymentStatus;
import br.com.pitflow.payment.core.exception.PaymentNotFoundException;
import br.com.pitflow.payment.core.exception.PaymentHomologationConflictException;
import br.com.pitflow.payment.core.gateway.PaymentGateway;
import br.com.pitflow.payment.core.gateway.PaymentStatusEventGateway;
import br.com.pitflow.payment.core.usecase.inputPort.RejectPaymentForHomologation;

import java.util.Comparator;
import java.util.UUID;

public final class RejectPaymentForHomologationImp implements RejectPaymentForHomologation {
    private final PaymentGateway payments;
    private final PaymentStatusEventGateway events;
    private final TransactionGateway tx;
    private final ClockGateway clock;

    public RejectPaymentForHomologationImp(PaymentGateway payments, PaymentStatusEventGateway events,
                                           TransactionGateway tx, ClockGateway clock) {
        this.payments = payments;
        this.events = events;
        this.tx = tx;
        this.clock = clock;
    }

    @Override
    public Result execute(UUID serviceOrderId) {
        if (serviceOrderId == null) throw new IllegalArgumentException("serviceOrderId is required");
        return tx.execute(() -> {
            Payment payment = payments.findByServiceOrderId(serviceOrderId).stream()
                    .max(Comparator.comparing(Payment::getCreatedAt))
                    .orElseThrow(() -> new PaymentNotFoundException(serviceOrderId));
            if (payment.getStatus() == PaymentStatus.REJECTED) {
                return new Result(payment.getId(), serviceOrderId, payment.getStatus(), true);
            }
            if (payment.getStatus() != PaymentStatus.CHECKOUT_PENDING
                    && payment.getStatus() != PaymentStatus.PENDING
                    && payment.getStatus() != PaymentStatus.IN_PROCESS) {
                throw new PaymentHomologationConflictException(
                        "Payment cannot be rejected from status " + payment.getStatus());
            }
            var now = clock.now();
            payment.reject(now);
            payments.save(payment);
            UUID sagaId = UUID.fromString(payment.getIdempotencyKey());
            events.rejected(new PaymentStatusEventGateway.Rejected(
                    UUID.randomUUID(), sagaId, sagaId, serviceOrderId, payment.getId(),
                    "homologation_rejection", "Pagamento rejeitado para demonstracao da compensacao", now));
            return new Result(payment.getId(), serviceOrderId, payment.getStatus(), false);
        });
    }
}
