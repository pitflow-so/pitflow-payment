package br.com.pitflow.payment.core.exception;

import br.com.pitflow.payment.core.enums.PaymentStatus;

public class InvalidPaymentStatusTransitionException extends RuntimeException {
    public InvalidPaymentStatusTransitionException(PaymentStatus from, PaymentStatus to) {
        super("Invalid payment status transition from " + from + " to " + to);
    }
}
