package br.com.pitflow.payment.infrastructure.web;

import br.com.pitflow.payment.controller.PaymentWebhookController;
import br.com.pitflow.payment.core.usecase.inputPort.ProcessMercadoPagoWebhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@ConditionalOnProperty(name = "payment.webhook.enabled", havingValue = "true")
@RequestMapping("/webhooks/mercado-pago")
@Tag(name = "Mercado Pago Webhook")
public class MercadoPagoWebhookRestAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MercadoPagoWebhookRestAdapter.class);
    private final MercadoPagoWebhookSignatureValidator signatures;
    private final PaymentWebhookController controller;
    private final ObjectMapper mapper;

    public MercadoPagoWebhookRestAdapter(MercadoPagoWebhookSignatureValidator signatures,
                                         PaymentWebhookController controller, ObjectMapper mapper) {
        this.signatures = signatures;
        this.controller = controller;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Recebe notificações assinadas de pagamento do Mercado Pago")
    public ResponseEntity<Map<String, String>> receive(
            @RequestHeader(value = "x-signature", required = false) String signature,
            @RequestHeader(value = "x-request-id", required = false) String requestId,
            @RequestParam(value = "data.id", required = false) String queryDataId,
            @RequestParam(value = "id", required = false) String legacyQueryDataId,
            @RequestParam(value = "type", required = false) String queryType,
            @RequestParam(value = "topic", required = false) String legacyQueryType,
            @RequestBody String rawPayload) {
        var root = mapper.readTree(rawPayload);
        String bodyType = root.path("type").asText();
        String bodyDataId = root.path("data").path("id").asText();
        String effectiveDataId = firstNonBlank(queryDataId, legacyQueryDataId, bodyDataId);
        String effectiveQueryType = firstNonBlank(queryType, legacyQueryType);
        if (!"payment".equals(bodyType)
                || (effectiveQueryType != null && !"payment".equals(effectiveQueryType))
                || bodyDataId.isBlank()
                || differs(queryDataId, bodyDataId)
                || differs(legacyQueryDataId, bodyDataId)) {
            return ResponseEntity.badRequest().body(Map.of("status", "invalid_payload"));
        }
        if (!signatures.isValid(signature, requestId, effectiveDataId)) {
            return ResponseEntity.status(401).body(Map.of("status", "invalid_signature"));
        }
        String notificationId = root.path("id").asText();
        String action = root.path("action").asText();
        if (notificationId.isBlank() || action.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("status", "invalid_payload"));
        }
        String eventKey = notificationId + ":" + action + ":" + bodyDataId;
        ProcessMercadoPagoWebhook.Result result = controller.mercadoPago(
                new ProcessMercadoPagoWebhook.Command(eventKey, notificationId, bodyDataId, action, rawPayload));
        if (result.status() == ProcessMercadoPagoWebhook.Status.IGNORED) {
            LOGGER.warn("Ignoring Mercado Pago payment not owned by PitFlow notificationId={} paymentId={} action={}",
                    notificationId, bodyDataId, action);
        }
        return ResponseEntity.ok(Map.of("status", result.status().name().toLowerCase()));
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static boolean differs(String queryValue, String bodyValue) {
        return queryValue != null
                && !queryValue.isBlank()
                && !queryValue.equals(bodyValue);
    }
}
