package br.com.pitflow.payment.core.gateway;

import java.util.UUID;

public interface OutboxEventGateway {
    OutboxEvent save(OutboxEvent event);

    record OutboxEvent(UUID aggregateId, String eventType, String payload) {
    }
}
