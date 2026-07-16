package br.com.pitflow.common.infrastructure.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    OpenAPI paymentOpenApi() {
        return new OpenAPI().info(new Info().title("PitFlow Payment API").version("v1"));
    }
}
