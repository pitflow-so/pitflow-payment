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
@Table(name = "webhook_events")
public class WebhookEventJpa {
    @Id
    private UUID id;
    @Column(name = "event_key", nullable = false)
    private String eventKey;
    @Column(nullable = false)
    private String provider;
    @Column(name = "provider_event_id")
    private String providerEventId;
    @Column(name = "provider_payment_id")
    private String providerPaymentId;
    private String action;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false)
    private int attempts;
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
    @Column(name = "processed_at")
    private Instant processedAt;
    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;
    @Column(name = "last_error")
    private String lastError;

    protected WebhookEventJpa() {
    }

    public WebhookEventJpa(UUID i, String k, String p, String pe, String pp, String a, String pl, String s, int at, Instant r, Instant pr, Instant n, String l) {
        id = i;
        eventKey = k;
        provider = p;
        providerEventId = pe;
        providerPaymentId = pp;
        action = a;
        payload = pl;
        status = s;
        attempts = at;
        receivedAt = r;
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
