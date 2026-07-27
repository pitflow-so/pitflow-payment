package br.com.pitflow.payment.core.gateway;

import java.time.Instant;

public interface WebhookEventGateway {
    boolean existsByEventKey(String eventKey);

    void save(WebhookEvent event);

    record WebhookEvent(String eventKey, String providerEventId, String providerPaymentId, String action,
                        String payload, Instant receivedAt, Instant processedAt) {
    }
}
