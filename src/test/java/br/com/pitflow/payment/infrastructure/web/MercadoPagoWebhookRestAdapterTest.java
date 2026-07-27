package br.com.pitflow.payment.infrastructure.web;

import br.com.pitflow.payment.controller.PaymentWebhookController;
import br.com.pitflow.payment.core.usecase.inputPort.ProcessMercadoPagoWebhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MercadoPagoWebhookRestAdapterTest {
    private PaymentWebhookController controller;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        controller = mock(PaymentWebhookController.class);
        mvc = MockMvcBuilders.standaloneSetup(new MercadoPagoWebhookRestAdapter(
                new MercadoPagoWebhookSignatureValidator("webhook-secret"), controller,
                JsonMapper.builder().build())).build();
    }

    @Test
    void rejectsInvalidSignatureBeforeProcessing() throws Exception {
        mvc.perform(post("/webhooks/mercado-pago")
                        .queryParam("data.id", "123456").queryParam("type", "payment")
                        .header("x-request-id", "req-abc").header("x-signature", "ts=1,v1=invalid")
                        .contentType(MediaType.APPLICATION_JSON).content(payload()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("invalid_signature"));
        verifyNoInteractions(controller);
    }

    @Test
    void acceptsSignedPaymentNotification() throws Exception {
        when(controller.mercadoPago(any())).thenReturn(new ProcessMercadoPagoWebhook.Result(
                ProcessMercadoPagoWebhook.Status.PROCESSED, UUID.randomUUID()));

        mvc.perform(post("/webhooks/mercado-pago")
                        .queryParam("data.id", "123456").queryParam("type", "payment")
                        .header("x-request-id", "req-abc")
                        .header("x-signature",
                                "ts=1742505638683,v1=8f3576c5918c00e6fb214b42d7d963dbe5ab1d129d35ba3c30d946898e250bde")
                        .contentType(MediaType.APPLICATION_JSON).content(payload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("processed"));
        verify(controller).mercadoPago(argThat(command -> "123456".equals(command.paymentId())
                && "payment.updated".equals(command.action())));
    }

    @Test
    void acceptsRealNotificationWithDataIdOnlyInBody() throws Exception {
        when(controller.mercadoPago(any())).thenReturn(new ProcessMercadoPagoWebhook.Result(
                ProcessMercadoPagoWebhook.Status.PROCESSED, UUID.randomUUID()));

        mvc.perform(post("/webhooks/mercado-pago")
                        .header("x-request-id", "req-abc")
                        .header("x-signature",
                                "ts=1742505638683,v1=8f3576c5918c00e6fb214b42d7d963dbe5ab1d129d35ba3c30d946898e250bde")
                        .contentType(MediaType.APPLICATION_JSON).content(payload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("processed"));
        verify(controller).mercadoPago(argThat(command ->
                "123456".equals(command.paymentId())));
    }

    @Test
    void rejectsConflictingQueryAndBodyPaymentIds() throws Exception {
        mvc.perform(post("/webhooks/mercado-pago")
                        .queryParam("data.id", "different")
                        .header("x-request-id", "req-abc")
                        .header("x-signature",
                                "ts=1742505638683,v1=8f3576c5918c00e6fb214b42d7d963dbe5ab1d129d35ba3c30d946898e250bde")
                        .contentType(MediaType.APPLICATION_JSON).content(payload()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("invalid_payload"));
        verifyNoInteractions(controller);
    }

    @Test
    void acceptsLegacyQueryNotificationWithReducedBody() throws Exception {
        when(controller.mercadoPago(any())).thenReturn(new ProcessMercadoPagoWebhook.Result(
                ProcessMercadoPagoWebhook.Status.PROCESSED, UUID.randomUUID()));

        mvc.perform(post("/webhooks/mercado-pago")
                        .queryParam("id", "123456")
                        .queryParam("topic", "payment")
                        .header("x-request-id", "req-abc")
                        .header("x-signature",
                                "ts=1742505638683,v1=8f3576c5918c00e6fb214b42d7d963dbe5ab1d129d35ba3c30d946898e250bde")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("processed"));
        verify(controller).mercadoPago(argThat(command ->
                "123456".equals(command.paymentId())
                        && "payment.updated".equals(command.action())));
    }

    private String payload() {
        return """
                {"id":"notification-1","type":"payment","action":"payment.updated",
                 "data":{"id":"123456"},"live_mode":false}
                """;
    }
}
