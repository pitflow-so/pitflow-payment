package br.com.pitflow.payment.core.gateway;

public interface PaymentProviderGateway {
    CheckoutPreferenceResult createCheckoutPreference(CheckoutPreferenceCommand command);

    ProviderPaymentResult findPaymentByProviderId(String id);

    record CheckoutPreferenceCommand(String externalReference) {
    }

    record CheckoutPreferenceResult(String preferenceId, String checkoutUrl) {
    }

    record ProviderPaymentResult(String providerPaymentId, String status) {
    }
}
