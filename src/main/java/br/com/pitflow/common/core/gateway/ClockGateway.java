package br.com.pitflow.common.core.gateway;

import java.time.Instant;

public interface ClockGateway {
    Instant now();
}
