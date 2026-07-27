package br.com.pitflow.payment.infrastructure.persistence;

import br.com.pitflow.payment.core.entity.Payment;
import br.com.pitflow.payment.infrastructure.persistence.entity.OutboxEventJpa;
import br.com.pitflow.payment.infrastructure.persistence.entity.PaymentAttemptJpa;
import br.com.pitflow.payment.infrastructure.persistence.entity.PaymentJpa;
import br.com.pitflow.payment.infrastructure.persistence.entity.WebhookEventJpa;
import br.com.pitflow.payment.infrastructure.persistence.mapper.PaymentMapper;
import br.com.pitflow.payment.infrastructure.persistence.repository.OutboxEventRepository;
import br.com.pitflow.payment.infrastructure.persistence.repository.PaymentAttemptRepository;
import br.com.pitflow.payment.infrastructure.persistence.repository.PaymentRepository;
import br.com.pitflow.payment.infrastructure.persistence.repository.WebhookEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "payment.mercado-pago.enabled=true",
        "payment.mercado-pago.access-token=test-access-token",
        "payment.mercado-pago.webhook-secret=test-webhook-secret",
        "payment.webhook.enabled=true"
})
@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlPersistenceIT {
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
    @Autowired
    PaymentRepository payments;
    @Autowired
    PaymentAttemptRepository attempts;
    @Autowired
    WebhookEventRepository webhooks;
    @Autowired
    OutboxEventRepository outbox;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    RestClient.Builder restClientBuilder;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    Payment payment(String key, String reference, UUID order, BigDecimal amount, String currency) {
        return Payment.create(UUID.randomUUID(), order, 1, reference, key, "hash", amount, currency, "payer@example.com", Instant.now());
    }

    @Test
    void migrationsHibernateValidationAndNumericPrecisionWork() {
        Payment saved = PaymentMapper.toDomain(payments.saveAndFlush(PaymentMapper.toJpa(payment("key-a", "ext-a", UUID.randomUUID(), new BigDecimal("123.45"), "BRL"))));
        assertThat(saved.getAmount()).isEqualByComparingTo("123.45");
    }

    @Test
    void restClientBuilderRequiredByMercadoPagoAdapterIsAvailable() {
        assertThat(restClientBuilder).isNotNull();
    }

    @Test
    void uniqueConstraintsWork() {
        UUID order = UUID.randomUUID();
        payments.saveAndFlush(PaymentMapper.toJpa(payment("key-b", "ext-b", order, BigDecimal.ONE, "BRL")));
        assertThatThrownBy(() -> payments.saveAndFlush(PaymentMapper.toJpa(payment("key-b", "ext-c", UUID.randomUUID(), BigDecimal.ONE, "BRL")))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void externalReferenceAndOrderVersionConstraintsWork() {
        UUID order = UUID.randomUUID();
        payments.saveAndFlush(PaymentMapper.toJpa(payment("key-c", "ext-unique", order, BigDecimal.ONE, "BRL")));
        assertThatThrownBy(() -> payments.saveAndFlush(PaymentMapper.toJpa(payment("key-d", "ext-unique", UUID.randomUUID(), BigDecimal.ONE, "BRL")))).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> payments.saveAndFlush(PaymentMapper.toJpa(payment("key-e", "ext-other", order, BigDecimal.ONE, "BRL")))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void amountAndCurrencyChecksWork() {
        PaymentJpa saved = payments.saveAndFlush(PaymentMapper.toJpa(payment("key-f", "ext-f", UUID.randomUUID(), BigDecimal.ONE, "BRL")));
        assertThatThrownBy(() -> jdbc.update("update payments set amount=0 where id=?", saved.getId())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("update payments set currency='USD' where id=?", saved.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void optimisticLockingWorks() {
        Payment domain = payment("key-g", "ext-g", UUID.randomUUID(), BigDecimal.ONE, "BRL");
        payments.saveAndFlush(PaymentMapper.toJpa(domain));
        jdbc.update("update payments set version=version+1 where id=?", domain.getId());
        assertThatThrownBy(() -> payments.saveAndFlush(PaymentMapper.toJpa(domain))).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void paymentAttemptPersists() {
        PaymentJpa p = payments.saveAndFlush(PaymentMapper.toJpa(payment("key-h", "ext-h", UUID.randomUUID(), BigDecimal.ONE, "BRL")));
        PaymentAttemptJpa a = attempts.saveAndFlush(new PaymentAttemptJpa(UUID.randomUUID(), p.getId(), "pref-1", null, "https://checkout.example", null, null, null, Instant.now(), Instant.now()));
        assertThat(attempts.findByPaymentId(p.getId())).extracting(PaymentAttemptJpa::getId).containsExactly(a.getId());
    }

    @Test
    void jsonbPersistsForInboxAndOutbox() {
        WebhookEventJpa w = webhooks.saveAndFlush(new WebhookEventJpa(UUID.randomUUID(), "evt", "MERCADO_PAGO", null, null, null, "{\"ok\":true}", "RECEIVED", 0, Instant.now(), null, null, null));
        OutboxEventJpa o = outbox.saveAndFlush(new OutboxEventJpa(UUID.randomUUID(), UUID.randomUUID(),
                "payment.created", "{\"ok\":true}", "PENDING", "queue", 0, Instant.now(), null, null, null));
        assertThat(webhooks.findById(w.getId()).orElseThrow().getPayload()).contains("true");
        assertThat(outbox.findById(o.getId()).orElseThrow().getPayload()).contains("true");
    }

    @Test
    void webhookEventKeyEnforcesInboxIdempotency() {
        Instant now = Instant.now();
        webhooks.saveAndFlush(new WebhookEventJpa(UUID.randomUUID(), "event-unique", "MERCADO_PAGO",
                "notification-1", "payment-1", "payment.updated", "{\"ok\":true}", "PROCESSED", 0,
                now, now, null, null));
        assertThat(webhooks.existsByEventKey("event-unique")).isTrue();
        assertThatThrownBy(() -> webhooks.saveAndFlush(new WebhookEventJpa(UUID.randomUUID(), "event-unique",
                "MERCADO_PAGO", "notification-1", "payment-1", "payment.updated", "{\"ok\":true}",
                "PROCESSED", 0, now, now, null, null))).isInstanceOf(DataIntegrityViolationException.class);
    }
}
