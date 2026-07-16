package br.com.pitflow.payment.infrastructure.persistence.repository;

import br.com.pitflow.payment.infrastructure.persistence.entity.PaymentJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentJpa, UUID> {
    Optional<PaymentJpa> findByIdempotencyKey(String key);

    Optional<PaymentJpa> findByExternalReference(String reference);

    List<PaymentJpa> findByServiceOrderId(UUID id);

    Optional<PaymentJpa> findByServiceOrderIdAndBudgetVersion(UUID id, long version);

    boolean existsByServiceOrderIdAndBudgetVersion(UUID id, long version);
}
