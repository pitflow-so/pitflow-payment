package br.com.pitflow.payment.infrastructure.persistence.adapter;

import br.com.pitflow.payment.core.entity.Payment;
import br.com.pitflow.payment.core.gateway.PaymentGateway;
import br.com.pitflow.payment.infrastructure.persistence.mapper.PaymentMapper;
import br.com.pitflow.payment.infrastructure.persistence.repository.PaymentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentPersistenceAdapter implements PaymentGateway {
    private final PaymentRepository repository;

    public PaymentPersistenceAdapter(PaymentRepository r) {
        repository = r;
    }

    public Payment save(Payment p) {
        return PaymentMapper.toDomain(repository.save(PaymentMapper.toJpa(p)));
    }

    public Optional<Payment> findById(UUID id) {
        return repository.findById(id).map(PaymentMapper::toDomain);
    }

    public Optional<Payment> findByIdempotencyKey(String k) {
        return repository.findByIdempotencyKey(k).map(PaymentMapper::toDomain);
    }

    public Optional<Payment> findByExternalReference(String r) {
        return repository.findByExternalReference(r).map(PaymentMapper::toDomain);
    }

    public List<Payment> findByServiceOrderId(UUID id) {
        return repository.findByServiceOrderId(id).stream().map(PaymentMapper::toDomain).toList();
    }

    public Optional<Payment> findByServiceOrderIdAndBudgetVersion(UUID id, long v) {
        return repository.findByServiceOrderIdAndBudgetVersion(id, v).map(PaymentMapper::toDomain);
    }

    public boolean existsByServiceOrderIdAndBudgetVersion(UUID id, long v) {
        return repository.existsByServiceOrderIdAndBudgetVersion(id, v);
    }
}
