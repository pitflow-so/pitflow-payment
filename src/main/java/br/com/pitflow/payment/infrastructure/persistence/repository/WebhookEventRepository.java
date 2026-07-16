package br.com.pitflow.payment.infrastructure.persistence.repository;

import br.com.pitflow.payment.infrastructure.persistence.entity.WebhookEventJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebhookEventRepository extends JpaRepository<WebhookEventJpa, UUID> {
}
