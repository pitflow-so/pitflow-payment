package br.com.pitflow.payment.infrastructure.persistence.mapper;

import br.com.pitflow.payment.core.entity.Payment;
import br.com.pitflow.payment.infrastructure.persistence.entity.PaymentJpa;

public final class PaymentMapper {
    private PaymentMapper() {
    }

    public static PaymentJpa toJpa(Payment p) {
        return new PaymentJpa(p.getId(), p.getServiceOrderId(), p.getBudgetVersion(), p.getExternalReference(), p.getIdempotencyKey(), p.getIdempotencyPayloadHash(), p.getAmount(), p.getCurrency(), p.getStatus(), p.getProvider(), p.getPayerEmail(), p.getApprovedAt(), p.getCreatedAt(), p.getUpdatedAt(), p.getVersion());
    }

    public static Payment toDomain(PaymentJpa p) {
        return new Payment(p.getId(), p.getServiceOrderId(), p.getBudgetVersion(), p.getExternalReference(), p.getIdempotencyKey(), p.getIdempotencyPayloadHash(), p.getAmount(), p.getCurrency(), p.getStatus(), p.getProvider(), p.getPayerEmail(), p.getApprovedAt(), p.getCreatedAt(), p.getUpdatedAt(), p.getVersion());
    }
}
