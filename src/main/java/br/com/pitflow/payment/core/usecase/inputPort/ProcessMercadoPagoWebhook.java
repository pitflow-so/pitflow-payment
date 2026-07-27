package br.com.pitflow.payment.core.usecase.inputPort;

public interface ProcessMercadoPagoWebhook {
    Result execute(Command command);

    record Command(String eventKey, String notificationId, String paymentId, String action, String rawPayload) {
    }

    record Result(Status status, java.util.UUID localPaymentId) {
    }

    enum Status {PROCESSED, DUPLICATE, IGNORED}
}
