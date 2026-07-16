package br.com.pitflow.payment.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventJpa {
    @Id
    private UUID id;
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;
    @Column(name = "event_type", nullable = false)
    private String eventType;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false)
    private int attempts;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "processed_at")
    private Instant processedAt;
    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;
    @Column(name = "last_error")
    private String lastError;

    protected OutboxEventJpa() {
    }

    public OutboxEventJpa(UUID i, UUID a, String e, String p, String s, int at, Instant c, Instant pr, Instant n, String l) {
        id = i;
        aggregateId = a;
        eventType = e;
        payload = p;
        status = s;
        attempts = at;
        createdAt = c;
        processedAt = pr;
        nextAttemptAt = n;
        lastError = l;
    }

    public UUID getId() {
        return id;
    }

    public String getPayload() {
        return payload;
    }
}
