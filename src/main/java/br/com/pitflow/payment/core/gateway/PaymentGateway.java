package br.com.pitflow.payment.core.gateway;

import br.com.pitflow.payment.core.entity.Payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentGateway {
    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);

    Optional<Payment> findByIdempotencyKey(String key);

    Optional<Payment> findByExternalReference(String reference);

    List<Payment> findByServiceOrderId(UUID id);

    Optional<Payment> findByServiceOrderIdAndBudgetVersion(UUID id, long version);

    boolean existsByServiceOrderIdAndBudgetVersion(UUID id, long version);
}
