package br.com.pitflow.payment.core.usecase.inputPort;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProcessCreatePayment {
    Result execute(Command command);

    record Command(UUID messageId, UUID sagaId, UUID correlationId, UUID serviceOrderId, BigDecimal amount,
                   String currency, String description, String idempotencyKey) {
    }

    record Result(UUID paymentId, String preferenceId, String checkoutUrl, boolean alreadyProcessed) {
    }
}
