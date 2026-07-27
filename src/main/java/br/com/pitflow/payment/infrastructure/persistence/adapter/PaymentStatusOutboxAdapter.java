package br.com.pitflow.payment.infrastructure.persistence.adapter;

import br.com.pitflow.payment.core.gateway.PaymentStatusEventGateway;
import br.com.pitflow.payment.infrastructure.persistence.entity.OutboxEventJpa;
import br.com.pitflow.payment.infrastructure.persistence.repository.OutboxEventRepository;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class PaymentStatusOutboxAdapter implements PaymentStatusEventGateway {
    private final OutboxEventRepository repository;
    private final ObjectMapper mapper;
    private final String destination;

    public PaymentStatusOutboxAdapter(OutboxEventRepository repository, ObjectMapper mapper,
                                      @Value("${aws.sqs.orchestrator-queue}") String destination) {
        this.repository = repository;
        this.mapper = mapper;
        this.destination = destination;
    }

    @Override
    public void approved(Approved event) {
        var payload = envelope(event.messageId(), "PaymentApproved", event.sagaId(), event.correlationId(),
                event.serviceOrderId(), event.occurredAt(), Map.of(
                        "paymentId", event.paymentId(),
                        "approvedAmount", Map.of("amount", event.amount().toPlainString(), "currency", event.currency()),
                        "externalPaymentId", event.externalPaymentId()
                ));
        save(event.paymentId(), "PaymentApproved", payload, event.occurredAt());
    }

    @Override
    public void rejected(Rejected event) {
        var eventPayload = new LinkedHashMap<String, Object>();
        eventPayload.put("paymentId", event.paymentId());
        eventPayload.put("reasonCode", event.reasonCode());
        eventPayload.put("reason", event.reason());
        var payload = envelope(event.messageId(), "PaymentRejected", event.sagaId(), event.correlationId(),
                event.serviceOrderId(), event.occurredAt(), eventPayload);
        save(event.paymentId(), "PaymentRejected", payload, event.occurredAt());
    }

    private Map<String, Object> envelope(UUID messageId, String type, UUID sagaId, UUID correlationId,
                                         UUID serviceOrderId, java.time.Instant occurredAt,
                                         Map<String, Object> eventPayload) {
        var envelope = new LinkedHashMap<String, Object>();
        envelope.put("schemaVersion", 1);
        envelope.put("messageId", messageId);
        envelope.put("type", type);
        envelope.put("occurredAt", occurredAt);
        envelope.put("correlationId", correlationId);
        envelope.put("sagaId", sagaId);
        envelope.put("serviceOrderId", serviceOrderId);
        envelope.put("payload", eventPayload);
        return envelope;
    }

    private void save(UUID paymentId, String type, Map<String, Object> payload, java.time.Instant occurredAt) {
        repository.save(new OutboxEventJpa(UUID.randomUUID(), paymentId, type, mapper.writeValueAsString(payload),
                "PENDING", destination, 0, occurredAt, null, occurredAt, null));
    }
}
