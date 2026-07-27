package br.com.pitflow.payment.controller;

import br.com.pitflow.payment.core.usecase.inputPort.ProcessMercadoPagoWebhook;

public class PaymentWebhookController {
    private final ProcessMercadoPagoWebhook processWebhook;

    public PaymentWebhookController(ProcessMercadoPagoWebhook processWebhook) {
        this.processWebhook = processWebhook;
    }

    public ProcessMercadoPagoWebhook.Result mercadoPago(ProcessMercadoPagoWebhook.Command command) {
        return processWebhook.execute(command);
    }
}
