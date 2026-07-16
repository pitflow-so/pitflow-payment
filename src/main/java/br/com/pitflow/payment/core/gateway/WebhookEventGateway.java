package br.com.pitflow.payment.core.gateway;

import java.util.Optional;

public interface WebhookEventGateway {
    WebhookEvent save(WebhookEvent event);

    Optional<WebhookEvent> findByEventKey(String key);

    record WebhookEvent(String eventKey, String payload) {
    }
}
