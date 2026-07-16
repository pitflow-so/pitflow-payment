package br.com.pitflow.payment.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_attempts")
public class PaymentAttemptJpa {
    @Id
    private UUID id;
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;
    @Column(name = "provider_preference_id", nullable = false)
    private String providerPreferenceId;
    @Column(name = "provider_payment_id")
    private String providerPaymentId;
    @Column(name = "checkout_url", nullable = false)
    private String checkoutUrl;
    @Column(name = "provider_status")
    private String providerStatus;
    @Column(name = "provider_status_detail")
    private String providerStatusDetail;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentAttemptJpa() {
    }

    public PaymentAttemptJpa(UUID i, UUID p, String pp, String pid, String u, String s, String d, Instant e, Instant c, Instant up) {
        id = i;
        paymentId = p;
        providerPreferenceId = pp;
        providerPaymentId = pid;
        checkoutUrl = u;
        providerStatus = s;
        providerStatusDetail = d;
        expiresAt = e;
        createdAt = c;
        updatedAt = up;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public String getProviderPreferenceId() {
        return providerPreferenceId;
    }

    public String getProviderPaymentId() {
        return providerPaymentId;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public String getProviderStatus() {
        return providerStatus;
    }

    public String getProviderStatusDetail() {
        return providerStatusDetail;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
