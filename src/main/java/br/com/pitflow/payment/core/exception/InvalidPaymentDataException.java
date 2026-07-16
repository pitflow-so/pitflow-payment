package br.com.pitflow.payment.core.exception;

public class InvalidPaymentDataException extends RuntimeException {
    public InvalidPaymentDataException(String message) {
        super(message);
    }
}
