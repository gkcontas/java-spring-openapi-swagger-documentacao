package com.gkcontas.openapi.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * The error body, described once and referenced from every operation.
 *
 * <p>Repeating an inline error schema per endpoint is how a document ends up with fifteen
 * slightly different error shapes, none of which matches what the service actually
 * returns. Declaring it once means a client generator produces one error type.
 *
 * <p>The shape is RFC 7807 — the same {@code ProblemDetail} Spring returns — so the
 * document describes the real response rather than an idealised one.
 */
@Schema(name = "ProblemDetail", description = "RFC 7807 problem details")
public record ApiErrorSchemas(

        @Schema(description = "A URI identifying the problem type", example = "about:blank")
        String type,

        @Schema(description = "Short, human-readable summary", example = "Product not found")
        String title,

        @Schema(description = "HTTP status code", example = "404")
        int status,

        @Schema(description = "Explanation specific to this occurrence",
                example = "No product with id 99 exists in the catalogue.")
        String detail,

        @Schema(description = "URI of the specific occurrence", example = "/api/v2/products/99")
        String instance,

        @Schema(description = """
                Present on validation failures only: one entry per violated constraint. An
                RFC 7807 extension member — the specification allows them, and this is the
                machine-readable form of what `detail` says in prose.
                """)
        List<ValidationError> errors) {

    @Schema(name = "ValidationError", description = "A single violated constraint")
    public record ValidationError(

            @Schema(description = "The offending field, or null for a rule spanning several fields",
                    example = "sku")
            String field,

            @Schema(description = "What is wrong with it", example = "must look like AB-123")
            String message) {
    }
}
