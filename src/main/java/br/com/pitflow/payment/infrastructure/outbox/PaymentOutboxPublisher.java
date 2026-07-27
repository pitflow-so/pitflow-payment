package br.com.pitflow.payment.infrastructure.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PaymentOutboxPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentOutboxPublisher.class);
    private final JdbcTemplate jdbc;
    private final SqsClient sqs;
    private final int batchSize;
    private final long leaseSeconds;
    private final int maxBackoffSeconds;

    public PaymentOutboxPublisher(JdbcTemplate jdbc, SqsClient sqs, int batchSize, long leaseSeconds,
                                  int maxBackoffSeconds) {
        this.jdbc = jdbc;
        this.sqs = sqs;
        this.batchSize = batchSize;
        this.leaseSeconds = leaseSeconds;
        this.maxBackoffSeconds = maxBackoffSeconds;
    }

    @Scheduled(fixedDelayString = "${payment.outbox.delay-ms:5000}")
    public void publishPending() {
        claim().forEach(this::publish);
    }

    @Transactional
    public List<Message> claim() {
        UUID lockId = UUID.randomUUID();
        return jdbc.query("""
                WITH candidates AS (
                    SELECT id
                    FROM outbox_events
                    WHERE (status = 'PENDING' AND COALESCE(next_attempt_at, created_at) <= now())
                       OR (status = 'IN_PROGRESS' AND locked_until < now())
                    ORDER BY created_at
                    FOR UPDATE SKIP LOCKED
                    LIMIT ?
                )
                UPDATE outbox_events o
                SET status = 'IN_PROGRESS',
                    lock_id = ?,
                    locked_until = now() + (? * interval '1 second')
                FROM candidates c
                WHERE o.id = c.id
                RETURNING o.id, o.destination, o.payload, o.attempts
                """, (rs, row) -> new Message(
                rs.getObject("id", UUID.class),
                rs.getString("destination"),
                rs.getString("payload"),
                rs.getInt("attempts")), batchSize, lockId, leaseSeconds);
    }

    private void publish(Message message) {
        try {
            String queueUrl = sqs.getQueueUrl(GetQueueUrlRequest.builder()
                    .queueName(message.destination()).build()).queueUrl();
            sqs.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(message.payload()).build());
            jdbc.update("""
                    UPDATE outbox_events
                    SET status='PUBLISHED', processed_at=now(), locked_until=NULL, lock_id=NULL, last_error=NULL
                    WHERE id=?
                    """, message.id());
        } catch (RuntimeException exception) {
            int attempts = message.attempts() + 1;
            int backoff = Math.min(maxBackoffSeconds, 1 << Math.min(attempts, 8));
            jdbc.update("""
                    UPDATE outbox_events
                    SET status='PENDING', attempts=?, next_attempt_at=?, locked_until=NULL, lock_id=NULL, last_error=?
                    WHERE id=?
                    """, attempts, Instant.now().plusSeconds(backoff), abbreviate(exception.getMessage()), message.id());
            LOGGER.warn("Payment outbox publication failed id={} attempts={}", message.id(), attempts);
        }
    }

    private static String abbreviate(String value) {
        if (value == null) return "Unknown publication error";
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }

    public record Message(UUID id, String destination, String payload, int attempts) {
    }
}
