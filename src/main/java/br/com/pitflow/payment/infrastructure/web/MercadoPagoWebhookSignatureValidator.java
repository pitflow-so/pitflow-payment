package br.com.pitflow.payment.infrastructure.web;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;

public class MercadoPagoWebhookSignatureValidator {
    private final byte[] secret;

    public MercadoPagoWebhookSignatureValidator(String secret) {
        if (secret == null || secret.isBlank()) throw new IllegalStateException("MERCADO_PAGO_WEBHOOK_SECRET is required");
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public boolean isValid(String signature, String requestId, String dataId) {
        if (blank(signature) || blank(requestId) || blank(dataId)) return false;
        String timestamp = part(signature, "ts");
        String received = part(signature, "v1");
        if (blank(timestamp) || blank(received)) return false;
        String manifest = "id:" + dataId.toLowerCase(Locale.ROOT) + ";request-id:" + requestId + ";ts:"
                + timestamp + ";";
        byte[] expected = hmac(manifest);
        byte[] actual = hex(received);
        return actual != null && MessageDigest.isEqual(expected, actual);
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
