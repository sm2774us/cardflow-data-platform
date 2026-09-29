package com.cardflow.platform.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** RFC 7807 problem responses; never leaks stack traces. */
@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    public static class BadRequest extends RuntimeException { public BadRequest(String m) { super(m); } }
    public static class Unauthorized extends RuntimeException { public Unauthorized(String m) { super(m); } }
    public static class NotFound extends RuntimeException { public NotFound(String m) { super(m); } }

    @ExceptionHandler(BadRequest.class)
    ProblemDetail badRequest(BadRequest e) { return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage()); }

    @ExceptionHandler(Unauthorized.class)
    ProblemDetail unauthorized(Unauthorized e) { return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage()); }

    @ExceptionHandler(NotFound.class)
    ProblemDetail notFound(NotFound e) { return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage()); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalid(MethodArgumentNotValidException e) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Validation failed");
        pd.setProperty("errors", e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).toList());
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadable(HttpMessageNotReadableException e) { return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed request body"); }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ProblemDetail unsupportedMedia(HttpMediaTypeNotSupportedException e) { return ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type must be application/json"); }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception e) {
        log.error("Unhandled error", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }
}
