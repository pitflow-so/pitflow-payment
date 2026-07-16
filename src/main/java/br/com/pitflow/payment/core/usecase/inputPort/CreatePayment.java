package br.com.pitflow.payment.core.usecase.inputPort;

import br.com.pitflow.payment.core.usecase.outputData.PaymentOutput;

import java.math.BigDecimal;
import java.util.UUID;

public interface CreatePayment {
    PaymentOutput execute(Command command);

    record Command(UUID serviceOrderId, long budgetVersion, BigDecimal amount, String currency, String payerEmail,
                   String idempotencyKey) {
    }
}
