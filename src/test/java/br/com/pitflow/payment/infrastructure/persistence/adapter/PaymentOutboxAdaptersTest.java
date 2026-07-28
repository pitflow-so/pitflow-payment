package br.com.pitflow.payment.infrastructure.persistence.adapter;

import br.com.pitflow.payment.core.gateway.PaymentLinkEventGateway;
import br.com.pitflow.payment.core.gateway.PaymentStatusEventGateway;
import br.com.pitflow.payment.infrastructure.persistence.entity.OutboxEventJpa;
import br.com.pitflow.payment.infrastructure.persistence.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentOutboxAdaptersTest {
    private final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final Instant now = Instant.parse("2026-07-27T12:00:00Z");

    @Test
    void storesPaymentLinkCreatedEnvelope() {
        var adapter = new PaymentLinkOutboxAdapter(repository, mapper, "orchestrator-queue");
        UUID paymentId = UUID.randomUUID();

        adapter.save(new PaymentLinkEventGateway.Event(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), paymentId, "pref-1", "https://checkout", new BigDecimal("10.00"), "BRL",
                now.plusSeconds(900), now));

        OutboxEventJpa event = captured();
        assertThat(event.getDestination()).isEqualTo("orchestrator-queue");
        assertThat(event.getPayload()).contains("\"schemaVersion\":1", "\"preferenceId\":\"pref-1\"",
                paymentId.toString());
    }

    @Test
    void storesApprovedAndRejectedEnvelopes() {
        var adapter = new PaymentStatusOutboxAdapter(repository, mapper, "orchestrator-queue");
        UUID paymentId = UUID.randomUUID();
        adapter.approved(new PaymentStatusEventGateway.Approved(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), paymentId, "external-1", new BigDecimal("12.30"), "BRL", now));
        assertThat(captured(1).getPayload()).contains("\"type\":\"PaymentApproved\"", "\"externalPaymentId\":\"external-1\"");

        adapter.rejected(new PaymentStatusEventGateway.Rejected(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), paymentId, "DECLINED", "Pagamento recusado", now));
        assertThat(captured(2).getPayload()).contains("\"type\":\"PaymentRejected\"", "\"reasonCode\":\"DECLINED\"");
    }

    private OutboxEventJpa captured() {
        return captured(1);
    }

    private OutboxEventJpa captured(int invocationCount) {
        ArgumentCaptor<OutboxEventJpa> captor = ArgumentCaptor.forClass(OutboxEventJpa.class);
        verify(repository, org.mockito.Mockito.times(invocationCount)).save(captor.capture());
        return captor.getAllValues().get(invocationCount - 1);
    }
}
