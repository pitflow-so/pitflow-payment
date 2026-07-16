package br.com.pitflow.payment.core.usecase;

import br.com.pitflow.payment.core.gateway.PaymentGateway;
import br.com.pitflow.payment.core.usecase.inputPort.FindPaymentByServiceOrderId;
import br.com.pitflow.payment.core.usecase.outputData.PaymentOutput;

import java.util.List;
import java.util.UUID;

public final class FindPaymentByServiceOrderIdImp implements FindPaymentByServiceOrderId {
    private final PaymentGateway gateway;

    public FindPaymentByServiceOrderIdImp(PaymentGateway g) {
        gateway = g;
    }

    public List<PaymentOutput> execute(UUID id) {
        return gateway.findByServiceOrderId(id).stream().map(PaymentOutput::from).toList();
    }
}
