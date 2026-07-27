package br.com.pitflow.payment.infrastructure.persistence.adapter;

import br.com.pitflow.payment.core.gateway.WebhookEventGateway;
import br.com.pitflow.payment.infrastructure.persistence.entity.WebhookEventJpa;
import br.com.pitflow.payment.infrastructure.persistence.repository.WebhookEventRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class WebhookEventPersistenceAdapter implements WebhookEventGateway {
    private final WebhookEventRepository repository;

    public WebhookEventPersistenceAdapter(WebhookEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByEventKey(String eventKey) {
        return repository.existsByEventKey(eventKey);
    }

    @Override
    public void save(WebhookEvent event) {
        repository.save(new WebhookEventJpa(UUID.randomUUID(), event.eventKey(), "MERCADO_PAGO",
                event.providerEventId(), event.providerPaymentId(), event.action(), event.payload(), "PROCESSED", 0,
                event.receivedAt(), event.processedAt(), null, null));
    }
}
