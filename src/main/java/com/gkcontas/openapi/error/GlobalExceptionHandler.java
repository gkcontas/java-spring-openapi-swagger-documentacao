package com.gkcontas.openapi.error;

import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Produces exactly the shape {@link ApiErrorSchemas} documents. If these two drift apart,
 * the document is lying — which is worse than having no document, because consumers trust it.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleNotFound(ProductNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Product not found", exception.getMessage());
    }

    /**
     * Catches {@link BindException} rather than {@code MethodArgumentNotValidException}.
     *
     * <p>The latter covers an invalid {@code @RequestBody}; a rejected query parameter
     * bound into a {@code @ParameterObject} record raises a plain {@code BindException}.
     * Since one extends the other, handling the parent covers both — handling only the
     * child leaves {@code ?size=500} falling through to a generic 500.
     *
     * <p>Every violation is reported, not the first one Hibernate Validator happened to
     * produce. Validation order is not defined, so returning one arbitrary message makes a
     * client fix a field, resubmit, and be told about the next one — and makes a test that
     * asserts a specific message pass or fail by luck.
     */
    @ExceptionHandler(BindException.class)
    public ProblemDetail handleInvalidRequest(BindException exception) {
        List<ValidationError> errors = exception.getAllErrors().stream()
                .map(GlobalExceptionHandler::toValidationError)
                .sorted(Comparator.comparing(ValidationError::field,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();

        String detail = errors.stream()
                .map(error -> error.field() == null
                        ? error.message()
                        : "%s %s".formatted(error.field(), error.message()))
                .reduce((left, right) -> left + "; " + right)
                .orElse("The request is not valid.");

        ProblemDetail problemDetail = problem(HttpStatus.BAD_REQUEST, "Invalid request", detail);
        // An RFC 7807 extension member: the machine-readable form of the same information,
        // so a caller does not have to parse the prose in `detail`.
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    private static ValidationError toValidationError(ObjectError error) {
        // A type-level constraint produces an ObjectError with no field. Forcing a field
        // name onto it is what makes an error message read "deliveryDetailValid requires…".
        return error instanceof FieldError fieldError
                ? new ValidationError(fieldError.getField(), fieldError.getDefaultMessage())
                : new ValidationError(null, error.getDefaultMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setTitle(title);
        problemDetail.setDetail(detail);
        return problemDetail;
    }

    /** One violation. Mirrors {@code ApiErrorSchemas.ValidationError} in the document. */
    public record ValidationError(String field, String message) {
    }
}
