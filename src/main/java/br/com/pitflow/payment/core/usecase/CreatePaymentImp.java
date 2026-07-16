package br.com.pitflow.payment.core.usecase;

import br.com.pitflow.common.core.gateway.ClockGateway;
import br.com.pitflow.common.core.gateway.PayloadHashGateway;
import br.com.pitflow.common.core.gateway.TransactionGateway;
import br.com.pitflow.payment.core.entity.Payment;
import br.com.pitflow.payment.core.exception.DuplicatePaymentException;
import br.com.pitflow.payment.core.exception.PaymentIdempotencyConflictException;
import br.com.pitflow.payment.core.gateway.PaymentGateway;
import br.com.pitflow.payment.core.usecase.inputPort.CreatePayment;
import br.com.pitflow.payment.core.usecase.outputData.PaymentOutput;

import java.util.UUID;

public final class CreatePaymentImp implements CreatePayment {
    private final PaymentGateway payments;
    private final TransactionGateway tx;
    private final ClockGateway clock;
    private final PayloadHashGateway hashes;

    public CreatePaymentImp(PaymentGateway p, TransactionGateway t, ClockGateway c, PayloadHashGateway h) {
        payments = p;
        tx = t;
        clock = c;
        hashes = h;
    }

    public PaymentOutput execute(Command c) {
        String canonical = c.serviceOrderId() + "|" + c.budgetVersion() + "|" + c.amount().toPlainString() + "|" + c.currency() + "|" + c.payerEmail();
        String hash = hashes.hash(canonical);
        return tx.execute(() -> payments.findByIdempotencyKey(c.idempotencyKey()).map(existing -> {
            if (!existing.getIdempotencyPayloadHash().equals(hash)) throw new PaymentIdempotencyConflictException();
            return PaymentOutput.from(existing);
        }).orElseGet(() -> {
            if (payments.existsByServiceOrderIdAndBudgetVersion(c.serviceOrderId(), c.budgetVersion()))
                throw new DuplicatePaymentException();
            UUID id = UUID.randomUUID();
            Payment p = Payment.create(id, c.serviceOrderId(), c.budgetVersion(), "payment:" + id, c.idempotencyKey(), hash, c.amount(), c.currency(), c.payerEmail(), clock.now());
            return PaymentOutput.from(payments.save(p));
        }));
    }
}
