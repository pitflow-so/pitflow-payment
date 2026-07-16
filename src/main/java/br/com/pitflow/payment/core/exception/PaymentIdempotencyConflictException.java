package br.com.pitflow.payment.core.exception;

public class PaymentIdempotencyConflictException extends RuntimeException {
    public PaymentIdempotencyConflictException() {
        super("The idempotency key was already used with different data");
    }
}
