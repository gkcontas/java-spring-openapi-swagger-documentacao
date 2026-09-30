package com.gkcontas.openapi.api.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "PhysicalProductV2", description = "A product that is shipped")
public record PhysicalProductV2Response(

        @Schema(description = "Catalogue identifier", example = "1")
        Long id,

        @Schema(description = "Display name", example = "Mechanical keyboard")
        String name,

        @Schema(description = "Stock keeping unit", example = "KB-001")
        String sku,

        Money price,

        @Schema(description = "Every category the product belongs to", example = "[\"peripherals\", \"input\"]")
        List<String> categories,

        @Schema(description = "Whether the product is still sold", example = "false")
        boolean discontinued,

        @Schema(description = "Shipping weight in kilograms", example = "1.2")
        Double weightKg) implements ProductV2Response {
}
