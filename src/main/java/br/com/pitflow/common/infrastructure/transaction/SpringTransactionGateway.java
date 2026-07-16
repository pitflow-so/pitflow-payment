package br.com.pitflow.common.infrastructure.transaction;

import br.com.pitflow.common.core.gateway.TransactionGateway;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
public class SpringTransactionGateway implements TransactionGateway {
    private final TransactionTemplate template;

    public SpringTransactionGateway(TransactionTemplate t) {
        template = t;
    }

    public <T> T execute(Supplier<T> operation) {
        return template.execute(status -> operation.get());
    }
}
