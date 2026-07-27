package br.com.pitflow.common.infrastructure.exception;

import br.com.pitflow.payment.core.exception.DuplicatePaymentException;
import br.com.pitflow.payment.core.exception.InvalidPaymentDataException;
import br.com.pitflow.payment.core.exception.InvalidPaymentStatusTransitionException;
import br.com.pitflow.payment.core.exception.PaymentIdempotencyConflictException;
import br.com.pitflow.payment.core.exception.PaymentNotFoundException;
import br.com.pitflow.payment.core.exception.PaymentHomologationConflictException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({InvalidPaymentDataException.class, MethodArgumentNotValidException.class})
    ResponseEntity<ApiError> bad(Exception e, HttpServletRequest r) {
        logFunctional("PAYMENT_INVALID_DATA", e, r);
        return response(HttpStatus.BAD_REQUEST, "PAYMENT_INVALID_DATA", e.getMessage(), r);
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    ResponseEntity<ApiError> missing(Exception e, HttpServletRequest r) {
        logFunctional("PAYMENT_NOT_FOUND", e, r);
        return response(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", e.getMessage(), r);
    }

    @ExceptionHandler({PaymentIdempotencyConflictException.class, DuplicatePaymentException.class})
    ResponseEntity<ApiError> conflict(Exception e, HttpServletRequest r) {
        String code = e instanceof PaymentIdempotencyConflictException
                ? "PAYMENT_IDEMPOTENCY_CONFLICT"
                : "PAYMENT_DUPLICATE";
        logFunctional(code, e, r);
        return response(HttpStatus.CONFLICT, code, e.getMessage(), r);
    }

    @ExceptionHandler(PaymentHomologationConflictException.class)
    ResponseEntity<ApiError> homologationConflict(Exception e, HttpServletRequest r) {
        logFunctional("PAYMENT_HOMOLOGATION_CONFLICT", e, r);
        return response(HttpStatus.CONFLICT, "PAYMENT_HOMOLOGATION_CONFLICT", e.getMessage(), r);
    }

    @ExceptionHandler(InvalidPaymentStatusTransitionException.class)
    ResponseEntity<ApiError> invalidState(Exception e, HttpServletRequest r) {
        logFunctional("PAYMENT_INVALID_STATUS_TRANSITION", e, r);
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "PAYMENT_INVALID_STATUS_TRANSITION", e.getMessage(), r);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception e, HttpServletRequest r) {
        LOGGER.error("Unexpected request failure method={} path={} exception={}",
                r.getMethod(), r.getRequestURI(), e.getClass().getSimpleName(), e);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", r);
    }

    private void logFunctional(String code, Exception exception, HttpServletRequest request) {
        LOGGER.warn("Request rejected code={} method={} path={} exception={} message={}",
                code, request.getMethod(), request.getRequestURI(),
                exception.getClass().getSimpleName(), exception.getMessage());
    }

    private ResponseEntity<ApiError> response(HttpStatus s, String c, String m, HttpServletRequest r) {
        return ResponseEntity.status(s).body(new ApiError(Instant.now(), s.value(), s.getReasonPhrase(), c, m, r.getRequestURI()));
    }
}
