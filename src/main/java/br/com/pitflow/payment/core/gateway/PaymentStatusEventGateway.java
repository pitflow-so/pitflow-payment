package br.com.pitflow.payment.core.gateway;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface PaymentStatusEventGateway {
    void approved(Approved event);

    void rejected(Rejected event);

    record Approved(UUID messageId, UUID sagaId, UUID correlationId, UUID serviceOrderId, UUID paymentId,
                    String externalPaymentId, BigDecimal amount, String currency, Instant occurredAt) {
    }

    record Rejected(UUID messageId, UUID sagaId, UUID correlationId, UUID serviceOrderId, UUID paymentId,
                    String reasonCode, String reason, Instant occurredAt) {
    }
}
