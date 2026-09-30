package com.gkcontas.openapi.api.admin;

import com.gkcontas.openapi.domain.ProductKind;
import com.gkcontas.openapi.validation.ConsistentDeliveryDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Validation constraints doubling as documentation.
 *
 * <p>springdoc reads the Bean Validation annotations and writes them into the schema as
 * {@code required}, {@code minLength}, {@code pattern} and {@code minimum}. The rule is
 * declared once and enforced at runtime, so the document cannot describe a limit the
 * service does not apply — the usual failure mode when the two are written separately.
 */
@Schema(name = "CreateProductRequest", description = "Payload for adding a product to the catalogue")
@ConsistentDeliveryDetail
public record CreateProductRequest(

        @Schema(description = "Display name", example = "Mechanical keyboard")
        @NotBlank(message = "must not be blank")
        @Size(min = 2, max = 120, message = "must be between 2 and 120 characters")
        String name,

        @Schema(description = "Stock keeping unit, two letters, a hyphen and three digits",
                example = "KB-042")
        @NotBlank(message = "must not be blank")
        @Pattern(regexp = "^[A-Z]{2}-\\d{3}$", message = "must look like AB-123")
        String sku,

        @Schema(description = "Price, greater than zero", example = "450.00")
        @NotNull(message = "must not be null")
        @DecimalMin(value = "0.01", message = "must be greater than zero")
        BigDecimal price,

        @Schema(description = "ISO 4217 currency code", example = "BRL", defaultValue = "BRL")
        @Pattern(regexp = "^[A-Z]{3}$", message = "must be a three-letter ISO 4217 code")
        String currency,

        @Schema(description = "Category slugs", example = "[\"peripherals\"]")
        List<String> categories,

        @Schema(description = "Whether the product ships or is downloaded", example = "PHYSICAL")
        @NotNull(message = "must not be null")
        ProductKind kind,

        @Schema(description = "Shipping weight in kilograms, required for physical products",
                example = "1.2")
        Double weightKg,

        @Schema(description = "Download location, required for digital products",
                example = "https://example.com/download/kb-042")
        String downloadUrl) {
}
