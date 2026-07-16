package br.com.pitflow.common.infrastructure.configuration;

import br.com.pitflow.common.core.gateway.ClockGateway;
import br.com.pitflow.common.core.gateway.PayloadHashGateway;
import br.com.pitflow.common.core.gateway.TransactionGateway;
import br.com.pitflow.payment.core.gateway.PaymentGateway;
import br.com.pitflow.payment.core.usecase.CreatePaymentImp;
import br.com.pitflow.payment.core.usecase.FindPaymentByIdImp;
import br.com.pitflow.payment.core.usecase.FindPaymentByServiceOrderIdImp;
import br.com.pitflow.payment.core.usecase.inputPort.CreatePayment;
import br.com.pitflow.payment.core.usecase.inputPort.FindPaymentById;
import br.com.pitflow.payment.core.usecase.inputPort.FindPaymentByServiceOrderId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Configuration
public class CoreConfiguration {
    @Bean
    ClockGateway clockGateway() {
        return Instant::now;
    }

    @Bean
    PayloadHashGateway payloadHashGateway() {
        return value -> {
            try {
                return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        };
    }

    @Bean
    CreatePayment createPayment(PaymentGateway p, TransactionGateway t, ClockGateway c, PayloadHashGateway h) {
        return new CreatePaymentImp(p, t, c, h);
    }

    @Bean
    FindPaymentById findPaymentById(PaymentGateway p) {
        return new FindPaymentByIdImp(p);
    }

    @Bean
    FindPaymentByServiceOrderId findPaymentByServiceOrderId(PaymentGateway p) {
        return new FindPaymentByServiceOrderIdImp(p);
    }
}
