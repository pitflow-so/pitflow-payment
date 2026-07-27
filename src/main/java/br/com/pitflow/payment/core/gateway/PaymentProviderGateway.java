package br.com.pitflow.payment.core.gateway;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public interface PaymentProviderGateway {
    Optional<CheckoutPreferenceResult> findCheckoutPreference(String externalReference);

    CheckoutPreferenceResult createCheckoutPreference(CheckoutPreferenceCommand command);

    ProviderPaymentResult findPaymentByProviderId(String id);

    record CheckoutPreferenceCommand(String externalReference, String title, BigDecimal amount, String currency,
                                     String notificationUrl, Instant expiresAt) {
    }

    record CheckoutPreferenceResult(String preferenceId, String checkoutUrl, Instant expiresAt) {
    }

    record ProviderPaymentResult(String providerPaymentId, String status, String statusDetail,
                                 String externalReference, BigDecimal amount, String currency,
                                 Instant approvedAt) {
    }
}
