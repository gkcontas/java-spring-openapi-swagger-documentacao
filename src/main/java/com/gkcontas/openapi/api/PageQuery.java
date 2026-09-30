package com.gkcontas.openapi.api;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Paging and filtering as one object instead of four loose {@code @RequestParam}s.
 *
 * <p>{@code @ParameterObject} tells springdoc to flatten this into individual query
 * parameters in the document, so the contract still shows {@code ?page=&size=&category=}
 * — the grouping is for the Java side, and the description of each parameter lives next
 * to the field it describes rather than in a growing method signature.
 *
 * <p>Related trap, not demonstrated here because the project does not use Spring Data:
 * passing Spring's {@code Pageable} directly produces a schema exposing the internal
 * class, with fields no caller can send. The fix is the same annotation —
 * {@code @ParameterObject Pageable} — plus springdoc's pageable converter.
 */
public record PageQuery(

        @Parameter(description = "Zero-based page index", example = "0")
        @Schema(defaultValue = "0", minimum = "0")
        @Min(value = 0, message = "must be at least 0")
        Integer page,

        @Parameter(description = "Number of items per page", example = "20")
        @Schema(defaultValue = "20", minimum = "1", maximum = "100")
        @Min(value = 1, message = "must be at least 1")
        @Max(value = 100, message = "must be at most 100")
        Integer size,

        @Parameter(description = "Filter by a category slug", example = "peripherals")
        String category) {

    public int pageOrDefault() {
        return page == null ? 0 : page;
    }

    public int sizeOrDefault() {
        return size == null ? 20 : size;
    }
}
