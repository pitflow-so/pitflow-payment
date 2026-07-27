package br.com.pitflow.payment.controller;

import br.com.pitflow.payment.core.usecase.inputPort.ProcessCreatePayment;

public class PaymentCommandController {
    private final ProcessCreatePayment processCreatePayment;

    public PaymentCommandController(ProcessCreatePayment processCreatePayment) {
        this.processCreatePayment = processCreatePayment;
    }

    public ProcessCreatePayment.Result createPayment(ProcessCreatePayment.Command command) {
        return processCreatePayment.execute(command);
    }
}
