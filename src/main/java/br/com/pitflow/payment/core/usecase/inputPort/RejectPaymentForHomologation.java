package br.com.pitflow.payment.core.usecase.inputPort;

import br.com.pitflow.payment.core.enums.PaymentStatus;

import java.util.UUID;

public interface RejectPaymentForHomologation {
    Result execute(UUID serviceOrderId);

    record Result(UUID paymentId, UUID serviceOrderId, PaymentStatus status, boolean alreadyRejected) {}
}
