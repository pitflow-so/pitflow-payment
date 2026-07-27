package br.com.pitflow.payment.infrastructure.consumer.sqs;

import br.com.pitflow.payment.controller.PaymentCommandController;
import br.com.pitflow.payment.core.usecase.inputPort.ProcessCreatePayment;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentCommandConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentCommandConsumer.class);
    private final SqsClient sqs;
    private final ObjectMapper mapper;
    private final PaymentCommandController controller;
    private final String queueUrl;
    private final int waitTimeSeconds;

    public PaymentCommandConsumer(SqsClient sqs, ObjectMapper mapper, PaymentCommandController controller,
                                  String queueName, int waitTimeSeconds) {
        this.sqs = sqs;
        this.mapper = mapper;
        this.controller = controller;
        this.waitTimeSeconds = waitTimeSeconds;
        this.queueUrl = sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).queueUrl();
    }

    @Scheduled(fixedDelayString = "${payment.consumer.delay-ms:1000}")
    public void poll() {
        var response = sqs.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl)
                .waitTimeSeconds(waitTimeSeconds).maxNumberOfMessages(10).build());
        response.messages().forEach(this::process);
    }

    void process(Message message) {
        try {
            JsonNode root = mapper.readTree(message.body());
            if (root.path("schemaVersion").asInt() != 1 || !"CreatePayment".equals(required(root, "type"))) {
                throw new IllegalArgumentException("Unsupported payment command");
            }
            JsonNode payload = root.path("payload");
            JsonNode amount = payload.path("amount");
            var result = controller.createPayment(new ProcessCreatePayment.Command(
                    UUID.fromString(required(root, "messageId")),
                    UUID.fromString(required(root, "sagaId")),
                    UUID.fromString(required(root, "correlationId")),
                    UUID.fromString(required(root, "serviceOrderId")),
                    new BigDecimal(required(amount, "amount")),
                    required(amount, "currency"),
                    required(payload, "description"),
                    required(payload, "idempotencyKey")
            ));
            sqs.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle()).build());
            LOGGER.info("CreatePayment processed messageId={} paymentId={} alreadyProcessed={}",
                    root.path("messageId").asText(), result.paymentId(), result.alreadyProcessed());
        } catch (RuntimeException exception) {
            LOGGER.warn("Payment command processing failed sqsMessageId={}", message.messageId(), exception);
        }
    }

    private static String required(JsonNode node, String field) {
        String value = node.path(field).asText();
        if (value.isBlank()) throw new IllegalArgumentException("Required field is missing: " + field);
        return value;
    }
}
