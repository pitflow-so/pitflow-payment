package br.com.pitflow.payment.infrastructure.persistence.entity;

import br.com.pitflow.payment.core.enums.PaymentProvider;
import br.com.pitflow.payment.core.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class PaymentJpa {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "service_order_id", nullable = false)
    private UUID serviceOrderId;
    @Column(name = "budget_version", nullable = false)
    private long budgetVersion;
    @Column(name = "external_reference", nullable = false)
    private String externalReference;
    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;
    @Column(name = "idempotency_payload_hash", nullable = false)
    private String idempotencyPayloadHash;
    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", columnDefinition = "char(3)", nullable = false)
    private String currency;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private PaymentProvider provider;
    @Column(name = "payer_email", nullable = false)
    private String payerEmail;
    @Column(name = "approved_at")
    private Instant approvedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PaymentJpa() {
    }

    public PaymentJpa(UUID id, UUID so, long bv, String er, String ik, String ih, BigDecimal a, String c, PaymentStatus s, PaymentProvider p, String pe, Instant aa, Instant ca, Instant ua, long v) {
        this.id = id;
        serviceOrderId = so;
        budgetVersion = bv;
        externalReference = er;
        idempotencyKey = ik;
        idempotencyPayloadHash = ih;
        amount = a;
        currency = c;
        status = s;
        provider = p;
        payerEmail = pe;
        approvedAt = aa;
        createdAt = ca;
        updatedAt = ua;
        version = v;
    }

    public UUID getId() {
        return id;
    }

    public UUID getServiceOrderId() {
        return serviceOrderId;
    }

    public long getBudgetVersion() {
        return budgetVersion;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getIdempotencyPayloadHash() {
        return idempotencyPayloadHash;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public PaymentProvider getProvider() {
        return provider;
    }

    public String getPayerEmail() {
        return payerEmail;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
