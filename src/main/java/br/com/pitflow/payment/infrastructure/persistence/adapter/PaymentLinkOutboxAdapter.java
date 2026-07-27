package br.com.pitflow.payment.infrastructure.persistence.adapter;

import br.com.pitflow.payment.core.gateway.PaymentLinkEventGateway;
import br.com.pitflow.payment.infrastructure.persistence.entity.OutboxEventJpa;
import br.com.pitflow.payment.infrastructure.persistence.repository.OutboxEventRepository;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class PaymentLinkOutboxAdapter implements PaymentLinkEventGateway {
    private final OutboxEventRepository repository;
    private final ObjectMapper mapper;
    private final String destination;

    public PaymentLinkOutboxAdapter(OutboxEventRepository repository, ObjectMapper mapper,
                                    @Value("${aws.sqs.orchestrator-queue}") String destination) {
        this.repository = repository;
        this.mapper = mapper;
        this.destination = destination;
    }

    @Override
    public void save(Event event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messageId", event.messageId());
        payload.put("type", "PaymentLinkCreated");
        payload.put("schemaVersion", 1);
        payload.put("sagaId", event.sagaId());
        payload.put("correlationId", event.correlationId());
        payload.put("serviceOrderId", event.serviceOrderId());
        payload.put("occurredAt", event.occurredAt());
        payload.put("payload", Map.of(
                "paymentId", event.paymentId(),
                "preferenceId", event.preferenceId(),
                "checkoutUrl", event.checkoutUrl(),
                "expiresAt", event.expiresAt().toString()
        ));
        repository.save(new OutboxEventJpa(UUID.randomUUID(), event.paymentId(), "PaymentLinkCreated",
                mapper.writeValueAsString(payload), "PENDING", destination, 0, event.occurredAt(), null,
                event.occurredAt(), null));
    }
}
