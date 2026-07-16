package br.com.pitflow.payment.infrastructure.persistence.repository;

import br.com.pitflow.payment.infrastructure.persistence.entity.PaymentAttemptJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttemptJpa, UUID> {
    List<PaymentAttemptJpa> findByPaymentId(UUID id);
}
