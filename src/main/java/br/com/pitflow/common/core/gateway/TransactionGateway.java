package br.com.pitflow.common.core.gateway;

import java.util.function.Supplier;

public interface TransactionGateway {
    <T> T execute(Supplier<T> operation);
}
