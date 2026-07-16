package br.com.pitflow.payment.core.gateway;

import java.util.UUID;

public interface ServiceOrderNotificationGateway {
    void notifyPaymentStatus(UUID paymentId);
}
