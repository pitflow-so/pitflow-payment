package br.com.pitflow.payment.core.exception;

public class PaymentHomologationConflictException extends RuntimeException {
    public PaymentHomologationConflictException(String message) {
        super(message);
    }
}
