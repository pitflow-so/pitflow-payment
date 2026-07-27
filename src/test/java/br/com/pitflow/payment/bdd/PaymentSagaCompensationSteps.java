package br.com.pitflow.payment.bdd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PaymentSagaCompensationSteps {

    private static final UUID TEST_CUSTOMER_ID =
            UUID.fromString("366941cf-9853-4514-ae99-1e1ea2b984ea");
    private static final String TEST_CUSTOMER_CPF = "78177454048";
    private static final UUID TEST_VEHICLE_ID =
            UUID.fromString("bdd00000-0000-4000-8000-000000000001");
    private static final String TEST_VEHICLE_PLATE = "BDD1A23";
    private static final UUID TEST_SERVICE_ID =
            UUID.fromString("3ad26f19-d339-446c-8185-e8bf4235ac1e");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private String apiUrl;
    private UUID serviceOrderId;
    private Duration timeout;
    private Duration pollInterval;
    private String customerToken;
    private String mechanicToken;
    private DynamoDbClient dynamoDb;
    private JsonNode rejection;
    private Map<String, AttributeValue> failedSaga;

    @Before
    public void configure() throws Exception {
        apiUrl = required("BDD_API_URL").replaceAll("/+$", "");
        timeout = Duration.ofSeconds(optionalInt("BDD_TIMEOUT_SECONDS", 120));
        pollInterval = Duration.ofSeconds(optionalInt("BDD_POLL_INTERVAL_SECONDS", 3));

        dynamoDb = DynamoDbClient.builder()
                .region(Region.of(optional("AWS_REGION", "us-east-1")))
                .build();
        customerToken = authenticateCustomer();
        mechanicToken = authenticateMechanic();
    }

    @After
    public void close() {
        if (dynamoDb != null) {
            dynamoDb.close();
        }
    }

    @Dado("que os dados acadêmicos de teste estão disponíveis")
    public void dadosAcademicosDisponiveis() throws Exception {
        var vehicleResponse = send("GET", "/registry/vehicles/" + TEST_VEHICLE_ID, null, customerToken);
        assertStatus(vehicleResponse, 200, "consulta do veículo acadêmico");
        var vehicle = objectMapper.readTree(vehicleResponse.body());
        assertEquals(TEST_VEHICLE_ID.toString(), vehicle.path("id").asText());
        assertEquals(TEST_CUSTOMER_ID.toString(), vehicle.path("customerId").asText());
        assertEquals(TEST_VEHICLE_PLATE, vehicle.path("licensePlate").asText());

        var serviceResponse = send("GET", "/inventory/services/" + TEST_SERVICE_ID, null, mechanicToken);
        assertStatus(serviceResponse, 200, "consulta do serviço acadêmico");
        var service = objectMapper.readTree(serviceResponse.body());
        assertEquals(TEST_SERVICE_ID.toString(), service.path("id").asText());
        assertTrue(service.path("price").decimalValue().signum() > 0,
                "O serviço acadêmico deve possuir preço positivo");
    }

    @Dado("que uma nova ordem de serviço aguarda pagamento")
    public void novaOrdemAguardaPagamento() throws Exception {
        var item = objectMapper.createObjectNode()
                .put("catalogId", TEST_SERVICE_ID.toString())
                .put("quantity", 1)
                .put("type", "SERVICE");
        var body = objectMapper.createObjectNode()
                .put("customerId", TEST_CUSTOMER_ID.toString())
                .put("vehicleId", TEST_VEHICLE_ID.toString())
                .put("orderDescription", "BDD E2E - compensação de pagamento");
        body.putArray("orderItems").add(item);

        var createResponse = send("POST", "/operation/service-orders/v2", body.toString(), customerToken);
        assertStatus(createResponse, 201, "criação da ordem de serviço");
        var createdOrder = objectMapper.readTree(createResponse.body());
        serviceOrderId = UUID.fromString(createdOrder.path("id").asText());
        assertEquals("RECEIVED", createdOrder.path("status").asText());

        assertNoContent(send("PATCH",
                "/operation/service-orders/" + serviceOrderId + "/start-diagnosis",
                null, mechanicToken), "início do diagnóstico");
        assertNoContent(send("PATCH",
                "/operation/service-orders/" + serviceOrderId + "/complete-diagnosis",
                null, mechanicToken), "conclusão do diagnóstico");
        awaitOperationStatus("AWAITING_APPROVAL");

        var approval = objectMapper.createObjectNode()
                .put("approved", true)
                .putNull("reason");
        assertNoContent(send("PATCH",
                "/operation/service-orders/v2/" + serviceOrderId + "/budget-decision",
                approval.toString(), customerToken), "aprovação do orçamento");
        awaitOperationStatus("AWAITING_PAYMENT");
    }

    @Quando("o pagamento dessa ordem for rejeitado")
    public void rejeitarPagamento() throws Exception {
        rejection = rejectPayment();
    }

    @Entao("o Payment deve permanecer em {string}")
    public void paymentDevePermanecer(String expectedStatus) {
        assertNotNull(rejection, "A resposta da rejeição não foi capturada");
        assertEquals(expectedStatus, rejection.path("status").asText());
        assertEquals(serviceOrderId.toString(), rejection.path("serviceOrderId").asText());
    }

    @Entao("a Operation deve chegar a {string}")
    public void operationDeveChegar(String expectedStatus) {
        awaitOperationStatus(expectedStatus);
    }

    @Entao("a SAGA deve terminar em {string}")
    public void sagaDeveTerminar(String expectedState) {
        await("SAGA em " + expectedState, () -> {
            var saga = findSaga();
            if (saga != null && expectedState.equals(text(saga, "state"))) {
                failedSaga = saga;
                return true;
            }
            return false;
        });
    }

    @Quando("a mesma rejeição for reenviada")
    public void reenviarRejeicao() throws Exception {
        assertNotNull(failedSaga, "A SAGA final deve ser capturada antes do replay");
        rejection = rejectPayment();
    }

    @Entao("a rejeição deve ser idempotente")
    public void rejeicaoDeveSerIdempotente() {
        assertTrue(rejection.path("alreadyRejected").asBoolean(),
                "O Payment deve identificar o replay da rejeição");
        assertEquals("REJECTED", rejection.path("status").asText());
        assertEquals("CANCELLED", unchecked(this::operationStatus));

        var sagaAfterReplay = findSaga();
        assertNotNull(sagaAfterReplay);
        assertEquals("FAILED", text(sagaAfterReplay, "state"));
        assertEquals(text(failedSaga, "version"), text(sagaAfterReplay, "version"),
                "O replay não pode criar uma nova transição da SAGA");
        assertEquals(text(failedSaga, "updatedAt"), text(sagaAfterReplay, "updatedAt"),
                "O replay não pode alterar a SAGA terminal");
    }

    private String authenticateCustomer() throws Exception {
        var body = objectMapper.createObjectNode().put("cpf", TEST_CUSTOMER_CPF);
        var response = send("POST", "/auth/customer", body.toString(), null);
        assertStatus(response, 200, "autenticação do cliente acadêmico");
        return requiredToken(response, "cliente");
    }

    private String authenticateMechanic() throws Exception {
        var body = objectMapper.createObjectNode()
                .put("username", required("BDD_MECHANIC_USERNAME"))
                .put("password", required("BDD_MECHANIC_PASSWORD"));
        var response = send("POST", "/registry/auth/login", body.toString(), null);
        assertStatus(response, 200, "autenticação do mecânico");
        return requiredToken(response, "mecânico");
    }

    private String requiredToken(HttpResponse<String> response, String actor) throws Exception {
        var token = objectMapper.readTree(response.body()).path("token").asText();
        assertTrue(!token.isBlank(), "A autenticação do " + actor + " não retornou token");
        return token;
    }

    private JsonNode rejectPayment() throws Exception {
        var response = send("POST",
                "/payment/homologation/service-orders/" + serviceOrderId + "/reject",
                null, mechanicToken);
        assertStatus(response, 200, "rejeição acadêmica do pagamento");
        return objectMapper.readTree(response.body());
    }

    private void awaitOperationStatus(String expectedStatus) {
        await("Operation em " + expectedStatus,
                () -> expectedStatus.equals(unchecked(this::operationStatus)));
    }

    private String operationStatus() throws Exception {
        var response = send("GET", "/operation/service-orders/" + serviceOrderId, null, mechanicToken);
        assertStatus(response, 200, "consulta da ordem de serviço");
        return objectMapper.readTree(response.body()).path("status").asText();
    }

    private HttpResponse<String> send(String method, String path, String body, String token)
            throws IOException, InterruptedException {
        var builder = HttpRequest.newBuilder(URI.create(apiUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json");
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        var publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        return httpClient.send(builder.method(method, publisher).build(), HttpResponse.BodyHandlers.ofString());
    }

    private Map<String, AttributeValue> findSaga() {
        var response = dynamoDb.query(QueryRequest.builder()
                .tableName(optional("BDD_ORCHESTRATOR_TABLE", "pitflow-orchestrator"))
                .indexName("by-service-order")
                .keyConditionExpression("GSI1PK = :pk")
                .expressionAttributeValues(Map.of(
                        ":pk", AttributeValue.builder().s("ORDER#" + serviceOrderId).build()))
                .build());
        return response.items().stream()
                .filter(item -> "SAGA".equals(text(item, "entityType")))
                .findFirst()
                .orElse(null);
    }

    private void await(String description, Supplier<Boolean> condition) {
        var deadline = Instant.now().plus(timeout);
        Throwable lastFailure = null;
        while (Instant.now().isBefore(deadline)) {
            try {
                if (condition.get()) {
                    return;
                }
            } catch (RuntimeException exception) {
                lastFailure = exception;
            }
            try {
                Thread.sleep(pollInterval.toMillis());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Polling interrompido: " + description, exception);
            }
        }
        var message = "Timeout aguardando " + description + " para a OS " + serviceOrderId;
        if (lastFailure != null) {
            throw new AssertionError(message, lastFailure);
        }
        throw new AssertionError(message);
    }

    private static String text(Map<String, AttributeValue> item, String attribute) {
        var value = item.get(attribute);
        if (value == null) {
            return null;
        }
        return value.s() != null ? value.s() : value.n();
    }

    private static void assertNoContent(HttpResponse<String> response, String operation) {
        assertStatus(response, 204, operation);
    }

    private static void assertStatus(HttpResponse<String> response, int expected, String operation) {
        assertEquals(expected, response.statusCode(),
                () -> "Falha na " + operation + ": HTTP " + response.statusCode() + " - " + response.body());
    }

    private static String required(String name) {
        var value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável obrigatória ausente: " + name);
        }
        return value;
    }

    private static String optional(String name, String defaultValue) {
        var value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static int optionalInt(String name, int defaultValue) {
        return Integer.parseInt(optional(name, Integer.toString(defaultValue)));
    }

    private static <T> T unchecked(CheckedSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
