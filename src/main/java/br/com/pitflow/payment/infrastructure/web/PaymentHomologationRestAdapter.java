package br.com.pitflow.payment.infrastructure.web;

import br.com.pitflow.payment.controller.PaymentHomologationController;
import br.com.pitflow.payment.core.usecase.inputPort.RejectPaymentForHomologation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/homologation/service-orders")
@Tag(name = "Payment Homologation")
@SecurityRequirement(name = "bearerAuth")
public class PaymentHomologationRestAdapter {
    private final PaymentHomologationController controller;

    public PaymentHomologationRestAdapter(PaymentHomologationController controller) {
        this.controller = controller;
    }

    @PostMapping("/{serviceOrderId}/reject")
    @Operation(summary = "Rejeita o pagamento para demonstrar a compensação da SAGA")
    ResponseEntity<RejectPaymentForHomologation.Result> reject(@PathVariable UUID serviceOrderId) {
        return ResponseEntity.ok(controller.reject(serviceOrderId));
    }
}
