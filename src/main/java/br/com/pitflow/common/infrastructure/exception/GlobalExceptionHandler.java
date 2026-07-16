package br.com.pitflow.common.infrastructure.exception;

import br.com.pitflow.payment.core.exception.DuplicatePaymentException;
import br.com.pitflow.payment.core.exception.InvalidPaymentDataException;
import br.com.pitflow.payment.core.exception.InvalidPaymentStatusTransitionException;
import br.com.pitflow.payment.core.exception.PaymentIdempotencyConflictException;
import br.com.pitflow.payment.core.exception.PaymentNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({InvalidPaymentDataException.class, MethodArgumentNotValidException.class})
    ResponseEntity<ApiError> bad(Exception e, HttpServletRequest r) {
        return response(HttpStatus.BAD_REQUEST, "PAYMENT_INVALID_DATA", e.getMessage(), r);
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    ResponseEntity<ApiError> missing(Exception e, HttpServletRequest r) {
        return response(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", e.getMessage(), r);
    }

    @ExceptionHandler({PaymentIdempotencyConflictException.class, DuplicatePaymentException.class})
    ResponseEntity<ApiError> conflict(Exception e, HttpServletRequest r) {
        return response(HttpStatus.CONFLICT, e instanceof PaymentIdempotencyConflictException ? "PAYMENT_IDEMPOTENCY_CONFLICT" : "PAYMENT_DUPLICATE", e.getMessage(), r);
    }

    @ExceptionHandler(InvalidPaymentStatusTransitionException.class)
    ResponseEntity<ApiError> invalidState(Exception e, HttpServletRequest r) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "PAYMENT_INVALID_STATUS_TRANSITION", e.getMessage(), r);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception e, HttpServletRequest r) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", r);
    }

    private ResponseEntity<ApiError> response(HttpStatus s, String c, String m, HttpServletRequest r) {
        return ResponseEntity.status(s).body(new ApiError(Instant.now(), s.value(), s.getReasonPhrase(), c, m, r.getRequestURI()));
    }
}
