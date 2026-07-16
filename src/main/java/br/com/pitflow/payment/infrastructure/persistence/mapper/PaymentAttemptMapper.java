package br.com.pitflow.payment.infrastructure.persistence.mapper;

import br.com.pitflow.payment.core.entity.PaymentAttempt;
import br.com.pitflow.payment.infrastructure.persistence.entity.PaymentAttemptJpa;

public final class PaymentAttemptMapper {
    private PaymentAttemptMapper() {
    }

    public static PaymentAttemptJpa toJpa(PaymentAttempt a) {
        return new PaymentAttemptJpa(a.id(), a.paymentId(), a.providerPreferenceId(), a.providerPaymentId(), a.checkoutUrl(), a.providerStatus(), a.providerStatusDetail(), a.expiresAt(), a.createdAt(), a.updatedAt());
    }

    public static PaymentAttempt toDomain(PaymentAttemptJpa a) {
        return new PaymentAttempt(a.getId(), a.getPaymentId(), a.getProviderPreferenceId(), a.getProviderPaymentId(), a.getCheckoutUrl(), a.getProviderStatus(), a.getProviderStatusDetail(), a.getExpiresAt(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
