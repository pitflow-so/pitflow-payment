package br.com.pitflow.payment.infrastructure.provider.mercadopago;

import br.com.pitflow.payment.core.gateway.PaymentProviderGateway;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MercadoPagoCheckoutAdapter implements PaymentProviderGateway {
    private final RestClient client;
    private final ObjectMapper mapper;
    private final boolean testMode;

    public MercadoPagoCheckoutAdapter(RestClient.Builder builder, ObjectMapper mapper, String baseUrl,
                                      String accessToken, boolean testMode) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("MERCADO_PAGO_ACCESS_TOKEN is required");
        }
        this.client = builder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build();
        this.mapper = mapper;
        this.testMode = testMode;
    }

    @Override
    public Optional<CheckoutPreferenceResult> findCheckoutPreference(String externalReference) {
        String json = client.get()
                .uri(uri -> uri.path("/checkout/preferences/search")
                        .queryParam("external_reference", externalReference)
                        .queryParam("limit", 1).build())
                .retrieve().body(String.class);
        JsonNode elements = read(json).path("elements");
        if (!elements.isArray() || elements.isEmpty()) return Optional.empty();
        return Optional.of(toResult(elements.get(0)));
    }

    @Override
    public CheckoutPreferenceResult createCheckoutPreference(CheckoutPreferenceCommand command) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("external_reference", command.externalReference());
        body.put("items", List.of(Map.of(
                "id", command.externalReference(),
                "title", command.title() == null || command.title().isBlank() ? "Ordem de servico PitFlow" : command.title(),
                "quantity", 1,
                "currency_id", command.currency(),
                "unit_price", command.amount()
        )));
        body.put("expires", true);
        body.put("expiration_date_from", Instant.now().toString());
        body.put("expiration_date_to", command.expiresAt().toString());
        if (command.notificationUrl() != null && !command.notificationUrl().isBlank()) {
            body.put("notification_url", command.notificationUrl());
        }
        String json = client.post().uri("/checkout/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(String.class);
        return toResult(read(json));
    }

    @Override
    public ProviderPaymentResult findPaymentByProviderId(String id) {
        String json = client.get().uri("/v1/payments/{id}", id).retrieve().body(String.class);
        JsonNode root = read(json);
        return new ProviderPaymentResult(root.path("id").asText(), root.path("status").asText());
    }

    private CheckoutPreferenceResult toResult(JsonNode node) {
        String url = testMode ? node.path("sandbox_init_point").asText() : node.path("init_point").asText();
        if (url.isBlank()) url = node.path("init_point").asText();
        if (node.path("id").asText().isBlank() || url.isBlank()) {
            throw new IllegalStateException("Mercado Pago returned an invalid preference");
        }
        String expiration = node.path("expiration_date_to").asText();
        return new CheckoutPreferenceResult(node.path("id").asText(), url,
                expiration.isBlank() ? null : Instant.parse(expiration));
    }

    private JsonNode read(String json) {
        try {
            return mapper.readTree(json);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Invalid Mercado Pago response", exception);
        }
    }
}
