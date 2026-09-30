package com.gkcontas.openapi.api.admin;

import com.gkcontas.openapi.api.v2.ProductV2Response;
import com.gkcontas.openapi.config.OpenApiConfig;
import com.gkcontas.openapi.domain.Product;
import com.gkcontas.openapi.domain.ProductStore;
import com.gkcontas.openapi.error.ApiErrorSchemas;
import com.gkcontas.openapi.error.ProductNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The internal API, in a group of its own.
 *
 * <p>{@code @SecurityRequirement} on the class is what marks every operation here as
 * protected and enables the padlock and the <em>Authorize</em> button in Swagger UI. The
 * scheme itself is declared once in {@link OpenApiConfig}; this only references it by
 * name, and a name that does not match a declared scheme fails silently — the padlock
 * simply never appears.
 *
 * <p>Note that the annotation documents the requirement, it does not enforce it: the
 * enforcement is in the security filter chain. Having both is the point, and a mismatch
 * between them is a documentation bug worth a test.
 */
@RestController
@RequestMapping(value = "/api/admin/products", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Admin", description = "Catalogue maintenance. Requires a bearer token.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class AdminProductController {

    private final ProductStore store;

    public AdminProductController(ProductStore store) {
        this.store = store;
    }

    @Operation(summary = "Create a product",
            description = """
                    Adds a product to the catalogue and returns it in the v2 representation.

                    Beyond the constraints shown in the schema, one conditional rule applies:
                    `weightKg` is required when `kind` is `PHYSICAL`, and `downloadUrl` when it is
                    `DIGITAL`. No annotation renders that into the schema, so it is stated here.
                    """)
    @ApiResponse(responseCode = "201", description = "The product was created")
    @ApiResponse(responseCode = "400", description = "The payload failed validation",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorSchemas.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorSchemas.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductV2Response create(@Valid @RequestBody CreateProductRequest request) {
        Product saved = store.save(new Product(null, request.name(), request.sku(), request.price(),
                request.currency() == null ? "BRL" : request.currency(),
                request.categories() == null ? List.of() : request.categories(),
                request.kind(), request.weightKg(), request.downloadUrl(), false));
        return ProductV2Response.from(saved);
    }

    @Operation(summary = "Delete a product",
            description = "Removes a product. Returns 204 with no body on success.")
    @ApiResponse(responseCode = "204", description = "The product was removed")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorSchemas.class)))
    @ApiResponse(responseCode = "404", description = "No product with that id",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorSchemas.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Catalogue identifier", example = "4", required = true)
            @PathVariable Long id) {
        if (!store.deleteById(id)) {
            throw new ProductNotFoundException(id);
        }
        return ResponseEntity.noContent().build();
    }
}
