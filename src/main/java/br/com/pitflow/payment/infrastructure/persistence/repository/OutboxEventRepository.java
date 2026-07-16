package br.com.pitflow.payment.infrastructure.persistence.repository;

import br.com.pitflow.payment.infrastructure.persistence.entity.OutboxEventJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventJpa, UUID> {
}
