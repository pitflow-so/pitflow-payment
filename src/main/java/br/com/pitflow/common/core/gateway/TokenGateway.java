package br.com.pitflow.common.core.gateway;

import java.util.Map;

public interface TokenGateway {
    String validateToken(String token);
    Map<String, Object> getClaims(String token);
}
