package com.gkcontas.openapi.api.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "DigitalProductV2", description = "A product delivered by download")
public record DigitalProductV2Response(

        @Schema(description = "Catalogue identifier", example = "3")
        Long id,

        @Schema(description = "Display name", example = "Design toolkit licence")
        String name,

        @Schema(description = "Stock keeping unit", example = "SW-100")
        String sku,

        Money price,

        @Schema(description = "Every category the product belongs to", example = "[\"software\"]")
        List<String> categories,

        @Schema(description = "Whether the product is still sold", example = "false")
        boolean discontinued,

        @Schema(description = "Where the licence can be downloaded",
                example = "https://example.com/download/sw-100", format = "uri")
        String downloadUrl) implements ProductV2Response {
}
