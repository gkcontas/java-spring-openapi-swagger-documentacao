package com.gkcontas.openapi.api.v1;

import com.gkcontas.openapi.domain.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(name = "ProductV1", description = "Flat product representation served by the deprecated v1 API")
public record ProductV1Response(

        @Schema(description = "Catalogue identifier", example = "1")
        Long id,

        @Schema(description = "Display name", example = "Mechanical keyboard")
        String name,

        @Schema(description = "Price in BRL. v2 replaces this with a money object carrying the currency.",
                example = "450.00", deprecated = true)
        BigDecimal price,

        @Schema(description = "The first category only. v2 returns the full list.",
                example = "peripherals", deprecated = true)
        String category) {

    public static ProductV1Response from(Product product) {
        return new ProductV1Response(product.id(), product.name(), product.price(),
                product.categories().isEmpty() ? null : product.categories().getFirst());
    }
}
