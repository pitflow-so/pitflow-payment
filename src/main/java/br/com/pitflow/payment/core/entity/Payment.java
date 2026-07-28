package br.com.pitflow.payment.core.entity;

import br.com.pitflow.payment.core.enums.PaymentProvider;
import br.com.pitflow.payment.core.enums.PaymentStatus;
import br.com.pitflow.payment.core.exception.InvalidPaymentDataException;
import br.com.pitflow.payment.core.exception.InvalidPaymentStatusTransitionException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Payment {
    private static final Map<PaymentStatus, Set<PaymentStatus>> TRANSITIONS = Map.of(
            PaymentStatus.CREATED, EnumSet.of(PaymentStatus.CHECKOUT_PENDING, PaymentStatus.CANCELLED, PaymentStatus.ERROR),
            PaymentStatus.CHECKOUT_PENDING, EnumSet.of(PaymentStatus.PENDING, PaymentStatus.IN_PROCESS, PaymentStatus.APPROVED, PaymentStatus.REJECTED, PaymentStatus.CANCELLED, PaymentStatus.EXPIRED, PaymentStatus.ERROR),
            PaymentStatus.PENDING, EnumSet.of(PaymentStatus.IN_PROCESS, PaymentStatus.APPROVED, PaymentStatus.REJECTED, PaymentStatus.CANCELLED, PaymentStatus.EXPIRED, PaymentStatus.ERROR),
            PaymentStatus.IN_PROCESS, EnumSet.of(PaymentStatus.PENDING, PaymentStatus.APPROVED, PaymentStatus.REJECTED, PaymentStatus.CANCELLED, PaymentStatus.EXPIRED, PaymentStatus.ERROR),
            PaymentStatus.APPROVED, EnumSet.of(PaymentStatus.REFUNDED),
            PaymentStatus.ERROR, EnumSet.of(PaymentStatus.CHECKOUT_PENDING, PaymentStatus.PENDING, PaymentStatus.IN_PROCESS, PaymentStatus.APPROVED, PaymentStatus.REJECTED, PaymentStatus.CANCELLED, PaymentStatus.EXPIRED)
    );
    private final UUID id;
    private final UUID serviceOrderId;
    private final long budgetVersion;
    private final String externalReference;
    private final String idempotencyKey;
    private final String idempotencyPayloadHash;
    private final BigDecimal amount;
    private final String currency;
    private final PaymentProvider provider;
    private final String payerEmail;
    private final Instant createdAt;
    private final long version;
    private PaymentStatus status;
    private Instant approvedAt;
    private Instant updatedAt;

    @SuppressWarnings("java:S107") // Rehydrates the complete persisted aggregate without mutable setters.
    public Payment(UUID id, UUID serviceOrderId, long budgetVersion, String externalReference, String idempotencyKey,
                   String idempotencyPayloadHash, BigDecimal amount, String currency, PaymentStatus status,
                   PaymentProvider provider, String payerEmail, Instant approvedAt, Instant createdAt, Instant updatedAt, long version) {
        this.id = required(id, "id");
        this.serviceOrderId = required(serviceOrderId, "serviceOrderId");
        if (budgetVersion <= 0) throw invalid("budgetVersion must be greater than zero");
        this.budgetVersion = budgetVersion;
        this.externalReference = text(externalReference, "externalReference");
        this.idempotencyKey = text(idempotencyKey, "idempotencyKey");
        this.idempotencyPayloadHash = text(idempotencyPayloadHash, "idempotencyPayloadHash");
        if (amount == null || amount.signum() <= 0) throw invalid("amount must be greater than zero");
        this.amount = amount;
        if (!"BRL".equals(currency)) throw invalid("currency must be BRL");
        this.currency = currency;
        this.status = required(status, "status");
        this.provider = required(provider, "provider");
        this.payerEmail = email(payerEmail);
        this.approvedAt = approvedAt;
        this.createdAt = required(createdAt, "createdAt");
        this.updatedAt = required(updatedAt, "updatedAt");
        this.version = version;
        if (status == PaymentStatus.APPROVED && approvedAt == null)
            throw invalid("approvedAt is required for approved payments");
    }

    @SuppressWarnings("java:S107") // Explicit factory contract keeps creation invariants inside the aggregate.
    public static Payment create(UUID id, UUID serviceOrderId, long budgetVersion, String externalReference, String idempotencyKey,
                                 String payloadHash, BigDecimal amount, String currency, String payerEmail, Instant now) {
        return new Payment(id, serviceOrderId, budgetVersion, externalReference, idempotencyKey, payloadHash, amount, currency,
                PaymentStatus.CREATED, PaymentProvider.MERCADO_PAGO, payerEmail, null, now, now, 0);
    }

    private static <T> T required(T value, String name) {
        if (value == null) throw invalid(name + " is required");
        return value;
    }

    private static String text(String value, String name) {
        if (value == null || value.isBlank()) throw invalid(name + " is required");
        return value;
    }

    private static String email(String value) {
        if (value == null || value.isBlank()) return null;
        text(value, "payerEmail");
        int at = value.indexOf('@');
        int dot = value.indexOf('.', at + 1);
        boolean invalidStructure = at <= 0
                || at != value.lastIndexOf('@')
                || dot <= at + 1
                || dot == value.length() - 1;
        if (invalidStructure || value.chars().anyMatch(Character::isWhitespace))
            throw invalid("payerEmail is invalid");
        return value;
    }

    private static InvalidPaymentDataException invalid(String message) {
        return new InvalidPaymentDataException(message);
    }

    public void markCheckoutPending(Instant now) {
        transition(PaymentStatus.CHECKOUT_PENDING, now);
    }

    public void markPending(Instant now) {
        transition(PaymentStatus.PENDING, now);
    }

    public void markInProcess(Instant now) {
        transition(PaymentStatus.IN_PROCESS, now);
    }

    public void approve(Instant now) {
        transition(PaymentStatus.APPROVED, now);
        approvedAt = now;
    }

    public void reject(Instant now) {
        transition(PaymentStatus.REJECTED, now);
    }

    public void cancel(Instant now) {
        transition(PaymentStatus.CANCELLED, now);
    }

    public void refund(Instant now) {
        transition(PaymentStatus.REFUNDED, now);
    }

    public void expire(Instant now) {
        transition(PaymentStatus.EXPIRED, now);
    }

    public void markError(Instant now) {
        transition(PaymentStatus.ERROR, now);
    }

    private void transition(PaymentStatus target, Instant now) {
        required(now, "updatedAt");
        if (!TRANSITIONS.getOrDefault(status, Set.of()).contains(target))
            throw new InvalidPaymentStatusTransitionException(status, target);
        status = target;
        updatedAt = now;
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
