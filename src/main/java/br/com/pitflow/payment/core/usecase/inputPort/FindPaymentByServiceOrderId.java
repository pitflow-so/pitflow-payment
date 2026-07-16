package br.com.pitflow.payment.core.usecase.inputPort;

import br.com.pitflow.payment.core.usecase.outputData.PaymentOutput;

import java.util.List;
import java.util.UUID;

public interface FindPaymentByServiceOrderId {
    List<PaymentOutput> execute(UUID id);
}
