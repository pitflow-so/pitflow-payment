package br.com.pitflow.payment.infrastructure.consumer.sqs;

import br.com.pitflow.payment.controller.PaymentCommandController;
import br.com.pitflow.payment.core.usecase.inputPort.ProcessCreatePayment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentCommandConsumerTest {
    private SqsClient sqs;
    private PaymentCommandController controller;
    private PaymentCommandConsumer consumer;

    @BeforeEach
    void setUp() {
        sqs = mock(SqsClient.class);
        controller = mock(PaymentCommandController.class);
        when(sqs.getQueueUrl(any(software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("queue-url").build());
        consumer = new PaymentCommandConsumer(sqs, new ObjectMapper(), controller, "payment-command", 2);
    }

    @Test
    void processesValidCommandAndDeletesMessage() {
        UUID paymentId = UUID.randomUUID();
        when(controller.createPayment(any())).thenReturn(
                new ProcessCreatePayment.Result(paymentId, "pref", "https://checkout", false));
        Message message = Message.builder().messageId("sqs-1").receiptHandle("receipt").body(validBody()).build();

        consumer.process(message);

        verify(controller).createPayment(argThat(command ->
                command.amount().toPlainString().equals("125.50")
                        && command.currency().equals("BRL")
                        && command.description().equals("Revisao")
                        && command.idempotencyKey().equals("payment-key")));
        verify(sqs).deleteMessage(any(software.amazon.awssdk.services.sqs.model.DeleteMessageRequest.class));
    }

    @Test
    void leavesInvalidOrFailedMessagesForSqsRetry() {
        Message invalid = Message.builder().messageId("sqs-2").receiptHandle("r2")
                .body("{\"schemaVersion\":2,\"type\":\"CreatePayment\"}").build();
        consumer.process(invalid);

        Message failed = Message.builder().messageId("sqs-3").receiptHandle("r3").body(validBody()).build();
        when(controller.createPayment(any())).thenThrow(new IllegalStateException("temporary failure"));
        consumer.process(failed);

        verify(sqs, never()).deleteMessage(any(software.amazon.awssdk.services.sqs.model.DeleteMessageRequest.class));
    }

    @Test
    void pollsAndProcessesReceivedMessages() {
        Message message = Message.builder().messageId("sqs-4").receiptHandle("r4").body(validBody()).build();
        when(controller.createPayment(any())).thenReturn(
                new ProcessCreatePayment.Result(UUID.randomUUID(), "pref", "url", true));
        when(sqs.receiveMessage(any(software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());

        consumer.poll();

        verify(sqs).deleteMessage(any(software.amazon.awssdk.services.sqs.model.DeleteMessageRequest.class));
    }

    private String validBody() {
        return """
                {
                  "schemaVersion": 1,
                  "messageId": "%s",
                  "type": "CreatePayment",
                  "sagaId": "%s",
                  "correlationId": "%s",
                  "serviceOrderId": "%s",
                  "payload": {
                    "amount": {"amount": "125.50", "currency": "BRL"},
                    "description": "Revisao",
                    "idempotencyKey": "payment-key"
                  }
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }
}
