package br.com.pitflow.payment.core.gateway;

import br.com.pitflow.payment.core.entity.PaymentAttempt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptGateway {
    PaymentAttempt save(PaymentAttempt attempt);

    List<PaymentAttempt> findByPaymentId(UUID paymentId);

    Optional<PaymentAttempt> findFirstByPaymentId(UUID paymentId);
}
