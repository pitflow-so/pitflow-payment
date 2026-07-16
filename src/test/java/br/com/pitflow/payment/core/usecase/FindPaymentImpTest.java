package br.com.pitflow.payment.core.usecase;

import br.com.pitflow.payment.core.entity.Payment;
import br.com.pitflow.payment.core.exception.PaymentNotFoundException;
import br.com.pitflow.payment.core.gateway.PaymentGateway;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FindPaymentImpTest {
    @Test
    void findsByIdAndOrder() {
        PaymentGateway g = mock(PaymentGateway.class);
        Payment p = Payment.create(UUID.randomUUID(), UUID.randomUUID(), 1, "e", "k", "h", BigDecimal.ONE, "BRL", "a@b.com", Instant.EPOCH);
        when(g.findById(p.getId())).thenReturn(Optional.of(p));
        when(g.findByServiceOrderId(p.getServiceOrderId())).thenReturn(List.of(p));
        assertThat(new FindPaymentByIdImp(g).execute(p.getId()).id()).isEqualTo(p.getId());
        assertThat(new FindPaymentByServiceOrderIdImp(g).execute(p.getServiceOrderId())).hasSize(1);
    }

    @Test
    void throwsWhenMissing() {
        PaymentGateway g = mock(PaymentGateway.class);
        UUID id = UUID.randomUUID();
        when(g.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new FindPaymentByIdImp(g).execute(id)).isInstanceOf(PaymentNotFoundException.class);
    }
}
