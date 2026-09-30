package com.gkcontas.openapi.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * The generic page wrapper, used by v1.
 *
 * <p>v2 deliberately does not use it: a generic wrapper erases its element type during
 * serialisation, which breaks a polymorphic payload. {@code ProductV2Page} explains the
 * mechanism in full.
 */
@Schema(description = "A page of results, with the totals needed to build pagination controls")
public record PagedResponse<T>(

        @Schema(description = "The items on this page")
        List<T> content,

        @Schema(description = "Zero-based index of this page", example = "0")
        int page,

        @Schema(description = "Requested page size", example = "20")
        int size,

        @Schema(description = "Total number of matching items across all pages", example = "42")
        long totalElements) {
}
