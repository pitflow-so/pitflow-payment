package br.com.pitflow.payment.core.entity;

import br.com.pitflow.payment.core.exception.InvalidPaymentDataException;

import java.time.Instant;
import java.util.UUID;

public record PaymentAttempt(UUID id, UUID paymentId, String providerPreferenceId, String providerPaymentId,
                             String checkoutUrl,
                             String providerStatus, String providerStatusDetail, Instant expiresAt, Instant createdAt,
                             Instant updatedAt) {
    public PaymentAttempt {
        if (id == null || paymentId == null || createdAt == null || updatedAt == null)
            throw new InvalidPaymentDataException("Payment attempt required data is missing");
        if ((providerPreferenceId == null) != (checkoutUrl == null))
            throw new InvalidPaymentDataException("providerPreferenceId and checkoutUrl must be supplied together");
    }
}
