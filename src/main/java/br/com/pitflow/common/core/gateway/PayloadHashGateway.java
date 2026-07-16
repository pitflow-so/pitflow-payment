package br.com.pitflow.common.core.gateway;

public interface PayloadHashGateway {
    String hash(String canonicalPayload);
}
