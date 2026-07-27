package br.com.pitflow.payment.infrastructure.persistence.adapter;

import br.com.pitflow.payment.core.entity.PaymentAttempt;
import br.com.pitflow.payment.core.gateway.PaymentAttemptGateway;
import br.com.pitflow.payment.infrastructure.persistence.mapper.PaymentAttemptMapper;
import br.com.pitflow.payment.infrastructure.persistence.repository.PaymentAttemptRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentAttemptPersistenceAdapter implements PaymentAttemptGateway {
    private final PaymentAttemptRepository repository;

    public PaymentAttemptPersistenceAdapter(PaymentAttemptRepository r) {
        repository = r;
    }

    public PaymentAttempt save(PaymentAttempt a) {
        return PaymentAttemptMapper.toDomain(repository.save(PaymentAttemptMapper.toJpa(a)));
    }

    public List<PaymentAttempt> findByPaymentId(UUID id) {
        return repository.findByPaymentId(id).stream().map(PaymentAttemptMapper::toDomain).toList();
    }

    public Optional<PaymentAttempt> findFirstByPaymentId(UUID id) {
        return repository.findFirstByPaymentIdOrderByCreatedAtDesc(id).map(PaymentAttemptMapper::toDomain);
    }
}
