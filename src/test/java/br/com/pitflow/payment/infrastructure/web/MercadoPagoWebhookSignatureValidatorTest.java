package br.com.pitflow.payment.infrastructure.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MercadoPagoWebhookSignatureValidatorTest {
    private final MercadoPagoWebhookSignatureValidator validator =
            new MercadoPagoWebhookSignatureValidator("webhook-secret");

    @Test
    void validatesOfficialManifestWithConstantTimeDigestComparison() {
        assertThat(validator.isValid(
                "ts=1742505638683,v1=8f3576c5918c00e6fb214b42d7d963dbe5ab1d129d35ba3c30d946898e250bde",
                "req-abc", "123456")).isTrue();
    }

    @Test
    void rejectsMissingMalformedOrDifferentSignature() {
        assertThat(validator.isValid(null, "req-abc", "123456")).isFalse();
        assertThat(validator.isValid("ts=1,v1=invalid", "req-abc", "123456")).isFalse();
        assertThat(validator.isValid(
                "ts=1742505638683,v1=8f3576c5918c00e6fb214b42d7d963dbe5ab1d129d35ba3c30d946898e250bde",
                "different", "123456")).isFalse();
    }
}
