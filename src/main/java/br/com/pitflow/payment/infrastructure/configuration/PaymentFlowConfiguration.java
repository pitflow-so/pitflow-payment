package br.com.pitflow.payment.infrastructure.configuration;

import br.com.pitflow.common.core.gateway.ClockGateway;
import br.com.pitflow.common.core.gateway.TransactionGateway;
import br.com.pitflow.payment.controller.PaymentCommandController;
import br.com.pitflow.payment.controller.PaymentWebhookController;
import br.com.pitflow.payment.core.gateway.PaymentAttemptGateway;
import br.com.pitflow.payment.core.gateway.PaymentGateway;
import br.com.pitflow.payment.core.gateway.PaymentLinkEventGateway;
import br.com.pitflow.payment.core.gateway.PaymentProviderGateway;
import br.com.pitflow.payment.core.usecase.ProcessCreatePaymentImp;
import br.com.pitflow.payment.core.usecase.ProcessMercadoPagoWebhookImp;
import br.com.pitflow.payment.core.usecase.inputPort.CreatePayment;
import br.com.pitflow.payment.core.usecase.inputPort.ProcessCreatePayment;
import br.com.pitflow.payment.core.usecase.inputPort.ProcessMercadoPagoWebhook;
import br.com.pitflow.payment.infrastructure.consumer.sqs.PaymentCommandConsumer;
import br.com.pitflow.payment.infrastructure.provider.mercadopago.MercadoPagoCheckoutAdapter;
import br.com.pitflow.payment.infrastructure.web.MercadoPagoWebhookSignatureValidator;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.jdbc.core.JdbcTemplate;
import br.com.pitflow.payment.infrastructure.outbox.PaymentOutboxPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class PaymentFlowConfiguration {
    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    SqsClient paymentSqsClient(@Value("${aws.region}") String region) {
        return SqsClient.builder().region(Region.of(region)).build();
    }

    @Bean
    @ConditionalOnProperty(name = "payment.mercado-pago.enabled", havingValue = "true")
    PaymentProviderGateway paymentProviderGateway(RestClient.Builder builder, ObjectMapper mapper,
            @Value("${payment.mercado-pago.base-url}") String baseUrl,
            @Value("${payment.mercado-pago.access-token}") String accessToken,
            @Value("${payment.mercado-pago.test-mode}") boolean testMode) {
        return new MercadoPagoCheckoutAdapter(builder, mapper, baseUrl, accessToken, testMode);
    }

    @Bean
    @ConditionalOnProperty(name = "payment.consumer.enabled", havingValue = "true")
    ProcessCreatePayment processCreatePayment(CreatePayment createPayment, PaymentGateway payments,
            PaymentAttemptGateway attempts, PaymentProviderGateway provider, PaymentLinkEventGateway events,
            TransactionGateway tx, ClockGateway clock,
            @Value("${payment.mercado-pago.notification-url}") String notificationUrl) {
        return new ProcessCreatePaymentImp(createPayment, payments, attempts, provider, events, tx, clock,
                notificationUrl);
    }

    @Bean
    @ConditionalOnProperty(name = "payment.consumer.enabled", havingValue = "true")
    PaymentCommandController paymentCommandController(ProcessCreatePayment useCase) {
        return new PaymentCommandController(useCase);
    }

    @Bean
    @ConditionalOnProperty(name = "payment.consumer.enabled", havingValue = "true")
    PaymentCommandConsumer paymentCommandConsumer(SqsClient sqs, ObjectMapper mapper,
            PaymentCommandController controller, @Value("${aws.sqs.payment-command-queue}") String queueName,
            @Value("${payment.consumer.wait-time-seconds}") int waitTimeSeconds) {
        return new PaymentCommandConsumer(sqs, mapper, controller, queueName, waitTimeSeconds);
    }

    @Bean
    @ConditionalOnProperty(name = "payment.outbox.enabled", havingValue = "true")
    PaymentOutboxPublisher paymentOutboxPublisher(JdbcTemplate jdbc, SqsClient sqs,
            @Value("${payment.outbox.batch-size}") int batchSize,
            @Value("${payment.outbox.lease-seconds}") long leaseSeconds,
            @Value("${payment.outbox.max-backoff-seconds}") int maxBackoffSeconds) {
        return new PaymentOutboxPublisher(jdbc, sqs, batchSize, leaseSeconds, maxBackoffSeconds);
    }

    @Bean
    @ConditionalOnProperty(name = "payment.webhook.enabled", havingValue = "true")
    MercadoPagoWebhookSignatureValidator mercadoPagoWebhookSignatureValidator(
            @Value("${payment.mercado-pago.webhook-secret}") String secret) {
        return new MercadoPagoWebhookSignatureValidator(secret);
    }

    @Bean
    @ConditionalOnProperty(name = "payment.webhook.enabled", havingValue = "true")
    ProcessMercadoPagoWebhook processMercadoPagoWebhook(
            br.com.pitflow.payment.core.gateway.WebhookEventGateway webhooks,
            PaymentProviderGateway provider, PaymentGateway payments, PaymentAttemptGateway attempts,
            br.com.pitflow.payment.core.gateway.PaymentStatusEventGateway events,
            TransactionGateway tx, ClockGateway clock) {
        return new ProcessMercadoPagoWebhookImp(webhooks, provider, payments, attempts, events, tx, clock);
    }

    @Bean
    @ConditionalOnProperty(name = "payment.webhook.enabled", havingValue = "true")
    PaymentWebhookController paymentWebhookController(ProcessMercadoPagoWebhook useCase) {
        return new PaymentWebhookController(useCase);
    }
}
