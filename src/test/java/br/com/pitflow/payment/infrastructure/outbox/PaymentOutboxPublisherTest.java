package br.com.pitflow.payment.infrastructure.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentOutboxPublisherTest {
    @Test
    void claimsAndPublishesPendingMessages() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        SqsClient sqs = mock(SqsClient.class);
        var message = new PaymentOutboxPublisher.Message(UUID.randomUUID(), "orchestrator", "{\"ok\":true}", 0);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of(message));
        when(sqs.getQueueUrl(any(software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("queue-url").build());

        new PaymentOutboxPublisher(jdbc, sqs, 10, 30, 300).publishPending();

        verify(sqs).sendMessage(any(software.amazon.awssdk.services.sqs.model.SendMessageRequest.class));
        verify(jdbc).update(contains("status='PUBLISHED'"), eq(message.id()));
    }

    @Test
    void reschedulesPublicationFailureWithBoundedError() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        SqsClient sqs = mock(SqsClient.class);
        String longError = "x".repeat(1100);
        var message = new PaymentOutboxPublisher.Message(UUID.randomUUID(), "orchestrator", "{}", 8);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of(message));
        when(sqs.getQueueUrl(any(software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest.class)))
                .thenThrow(new IllegalStateException(longError));

        new PaymentOutboxPublisher(jdbc, sqs, 10, 30, 60).publishPending();

        verify(jdbc).update(contains("status='PENDING'"), eq(9), any(), argThat(value ->
                value instanceof String text && text.length() == 1000), eq(message.id()));
        verify(sqs, never()).sendMessage(any(software.amazon.awssdk.services.sqs.model.SendMessageRequest.class));
    }

    @Test
    void exposesClaimedMessages() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        SqsClient sqs = mock(SqsClient.class);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());

        assertThat(new PaymentOutboxPublisher(jdbc, sqs, 5, 20, 60).claim()).isEmpty();
    }
}
