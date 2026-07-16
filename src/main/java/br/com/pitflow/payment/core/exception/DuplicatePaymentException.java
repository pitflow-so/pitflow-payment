package br.com.pitflow.payment.core.exception;

public class DuplicatePaymentException extends RuntimeException {
    public DuplicatePaymentException() {
        super("A payment already exists for this service order and budget version");
    }
}
