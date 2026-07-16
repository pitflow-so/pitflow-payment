package br.com.pitflow.payment.core.usecase.inputPort;

import br.com.pitflow.payment.core.usecase.outputData.PaymentOutput;

import java.util.UUID;

public interface FindPaymentById {
    PaymentOutput execute(UUID id);
}
