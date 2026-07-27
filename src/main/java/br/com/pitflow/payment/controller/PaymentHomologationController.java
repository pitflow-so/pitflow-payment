package br.com.pitflow.payment.controller;

import br.com.pitflow.payment.core.usecase.inputPort.RejectPaymentForHomologation;

import java.util.UUID;

public final class PaymentHomologationController {
    private final RejectPaymentForHomologation rejectPayment;

    public PaymentHomologationController(RejectPaymentForHomologation rejectPayment) {
        this.rejectPayment = rejectPayment;
    }

    public RejectPaymentForHomologation.Result reject(UUID serviceOrderId) {
        return rejectPayment.execute(serviceOrderId);
    }
}
