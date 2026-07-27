package br.com.pitflow.common.infrastructure.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void unexpectedFailureDoesNotExposeInternalDetailsInResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST",
                "/payment/webhooks/mercado-pago");
        RuntimeException exception = new RuntimeException("provider token and internal details");

        var response = new GlobalExceptionHandler().unexpected(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().message()).doesNotContain("provider token");
    }
}
