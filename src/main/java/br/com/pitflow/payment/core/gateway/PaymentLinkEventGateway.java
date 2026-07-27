package br.com.pitflow.payment.core.gateway;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface PaymentLinkEventGateway {
    void save(Event event);

    record Event(UUID messageId, UUID sagaId, UUID correlationId, UUID serviceOrderId, UUID paymentId,
                 String preferenceId, String checkoutUrl, BigDecimal amount, String currency, Instant expiresAt,
                 Instant occurredAt) {
    }
}
