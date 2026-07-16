package br.com.pitflow.payment.core.usecase;

import br.com.pitflow.payment.core.exception.PaymentNotFoundException;
import br.com.pitflow.payment.core.gateway.PaymentGateway;
import br.com.pitflow.payment.core.usecase.inputPort.FindPaymentById;
import br.com.pitflow.payment.core.usecase.outputData.PaymentOutput;

import java.util.UUID;

public final class FindPaymentByIdImp implements FindPaymentById {
    private final PaymentGateway gateway;

    public FindPaymentByIdImp(PaymentGateway g) {
        gateway = g;
    }

    public PaymentOutput execute(UUID id) {
        return gateway.findById(id).map(PaymentOutput::from).orElseThrow(() -> new PaymentNotFoundException(id));
    }
}
