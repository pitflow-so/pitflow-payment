package br.com.pitflow.payment.infrastructure.web;

import com.mercadopago.exceptions.MPInvalidWebhookSignatureException;
import com.mercadopago.webhook.WebhookSignatureValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class MercadoPagoWebhookSignatureValidator {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(MercadoPagoWebhookSignatureValidator.class);

    private final String secretValue;
    private final byte[] secret;

    public MercadoPagoWebhookSignatureValidator(String secret) {
        if (secret == null || secret.isBlank()) throw new IllegalStateException("MERCADO_PAGO_WEBHOOK_SECRET is required");
        this.secretValue = secret;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public boolean isValid(String signature, String requestId, String dataId) {
        if (blank(signature) || blank(requestId) || blank(dataId)) return false;

        try {
            WebhookSignatureValidator.validate(signature, requestId, dataId, secretValue);
            return true;
        } catch (MPInvalidWebhookSignatureException exception) {
            logDiagnostic(signature, requestId, dataId, exception);
            return false;
        }
    }

    private void logDiagnostic(
            String signature,
            String requestId,
            String dataId,
            MPInvalidWebhookSignatureException exception
    ) {
        String timestamp = part(signature, "ts");
        String received = part(signature, "v1");
        if (blank(timestamp) || blank(received)) {
            LOGGER.warn(
                    "Mercado Pago signature diagnostic reason={} requestId={} dataId={} "
                            + "timestampPresent={} hashPresent={}",
                    exception.getReason(),
                    requestId,
                    dataId,
                    !blank(timestamp),
                    !blank(received)
            );
            return;
        }

        Map<String, String> candidates = new LinkedHashMap<>();
        candidates.put("official",
                "id:" + dataId + ";request-id:" + requestId + ";ts:" + timestamp + ";");
        candidates.put("official-lowercase-id",
                "id:" + dataId.toLowerCase(Locale.ROOT)
                        + ";request-id:" + requestId + ";ts:" + timestamp + ";");
        candidates.put("without-data-id",
                "request-id:" + requestId + ";ts:" + timestamp + ";");
        candidates.put("without-request-id",
                "id:" + dataId + ";ts:" + timestamp + ";");

        String matchingCandidate = candidates.entrySet().stream()
                .filter(entry -> digestMatches(entry.getValue(), received))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("none");

        String expected = digest(candidates.get("official"));
        LOGGER.warn(
                "Mercado Pago signature diagnostic reason={} requestId={} dataId={} ts={} "
                        + "receivedPrefix={} expectedPrefix={} matchingCandidate={}",
                exception.getReason(),
                requestId,
                dataId,
                timestamp,
                prefix(received),
                prefix(expected),
                matchingCandidate
        );
    }

    private byte[] hmac(String manifest) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }

    private String digest(String manifest) {
        return HexFormat.of().formatHex(hmac(manifest));
    }

    private boolean digestMatches(String manifest, String received) {
        byte[] actual = hex(received);
        return actual != null
                && MessageDigest.isEqual(hmac(manifest), actual);
    }

    private static String prefix(String value) {
        if (blank(value)) return "missing";
        return value.substring(0, Math.min(12, value.length()));
    }

    private static String part(String signature, String name) {
        return Arrays.stream(signature.split(","))
                .map(String::trim)
                .filter(value -> value.startsWith(name + "="))
                .map(value -> value.substring(name.length() + 1).trim())
                .findFirst().orElse(null);
    }

    private static byte[] hex(String value) {
        if (value.length() != 64) return null;
        try {
            return java.util.HexFormat.of().parseHex(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
