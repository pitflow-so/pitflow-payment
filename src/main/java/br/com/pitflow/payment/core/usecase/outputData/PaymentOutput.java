package br.com.pitflow.payment.core.usecase.outputData;

import br.com.pitflow.payment.core.entity.Payment;
import br.com.pitflow.payment.core.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentOutput(UUID id, UUID serviceOrderId, long budgetVersion, BigDecimal amount, String currency,
                            PaymentStatus status) {
    public static PaymentOutput from(Payment p) {
        return new PaymentOutput(p.getId(), p.getServiceOrderId(), p.getBudgetVersion(), p.getAmount(), p.getCurrency(), p.getStatus());
    }
}
