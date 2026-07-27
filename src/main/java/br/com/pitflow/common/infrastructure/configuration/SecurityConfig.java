package br.com.pitflow.common.infrastructure.configuration;

import br.com.pitflow.common.core.gateway.TokenGateway;
import br.com.pitflow.common.infrastructure.security.JwtServiceImp;
import br.com.pitflow.common.infrastructure.security.SecurityFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    TokenGateway tokenGateway(@Value("${api.security.token.secret}") String secret) {
        return new JwtServiceImp(secret);
    }

    @Bean
    SecurityFilter securityFilter(TokenGateway tokens) {
        return new SecurityFilter(tokens);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityFilter filter) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/metrics",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/webhooks/mercado-pago").permitAll()
                        .requestMatchers("/homologation/**").hasRole("MECHANIC")
                        .anyRequest().authenticated())
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
