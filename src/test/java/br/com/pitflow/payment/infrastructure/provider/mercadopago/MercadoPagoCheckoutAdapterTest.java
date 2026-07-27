package br.com.pitflow.payment.infrastructure.provider.mercadopago;

import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MercadoPagoCheckoutAdapterTest {
    @Test
    void searchesByStableExternalReferenceBeforeCreating() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new MercadoPagoCheckoutAdapter(builder, JsonMapper.builder().build(), "https://api.test",
                "TEST-token", true);
        server.expect(once(), requestTo("https://api.test/checkout/preferences/search?external_reference=payment:123&limit=1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer TEST-token"))
                .andRespond(withSuccess("""
                        {"elements":[{"id":"pref-existing","init_point":"https://prod/pay",
                        "sandbox_init_point":"https://sandbox/pay","expiration_date_to":"2026-07-28T00:00:00Z"}]}
                        """, MediaType.APPLICATION_JSON));

        var result = adapter.findCheckoutPreference("payment:123").orElseThrow();

        assertThat(result.preferenceId()).isEqualTo("pref-existing");
        assertThat(result.checkoutUrl()).isEqualTo("https://sandbox/pay");
        server.verify();
    }

    @Test
    void createsTestPreferenceWithAmountAndExpiration() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new MercadoPagoCheckoutAdapter(builder, JsonMapper.builder().build(), "https://api.test",
                "TEST-token", true);
        server.expect(once(), requestTo("https://api.test/checkout/preferences"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer TEST-token"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.external_reference").value("payment:456"))
                .andExpect(jsonPath("$.items[0].unit_price").value(450.00))
                .andExpect(jsonPath("$.expires").value(true))
                .andRespond(withSuccess("""
                        {"id":"pref-new","init_point":"https://prod/pay",
                        "sandbox_init_point":"https://sandbox/new","expiration_date_to":"2026-07-28T00:00:00Z"}
                        """, MediaType.APPLICATION_JSON));

        var result = adapter.createCheckoutPreference(new br.com.pitflow.payment.core.gateway.PaymentProviderGateway
                .CheckoutPreferenceCommand("payment:456", "OS 456", new BigDecimal("450.00"), "BRL", "",
                Instant.parse("2026-07-28T00:00:00Z")));

        assertThat(result.checkoutUrl()).isEqualTo("https://sandbox/new");
        server.verify();
    }

    @Test
    void obtainsAuthoritativePaymentData() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new MercadoPagoCheckoutAdapter(builder, JsonMapper.builder().build(), "https://api.test",
                "TEST-token", true);
        server.expect(once(), requestTo("https://api.test/v1/payments/987"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer TEST-token"))
                .andRespond(withSuccess("""
                        {"id":987,"status":"approved","status_detail":"accredited",
                        "external_reference":"payment:123","transaction_amount":450.00,
                        "currency_id":"BRL","date_approved":"2026-07-27T01:00:00Z"}
                        """, MediaType.APPLICATION_JSON));

        var result = adapter.findPaymentByProviderId("987");

        assertThat(result.status()).isEqualTo("approved");
        assertThat(result.externalReference()).isEqualTo("payment:123");
        assertThat(result.amount()).isEqualByComparingTo("450.00");
        server.verify();
    }
}
