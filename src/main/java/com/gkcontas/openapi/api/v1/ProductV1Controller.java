package com.gkcontas.openapi.api.v1;

import com.gkcontas.openapi.api.PageQuery;
import com.gkcontas.openapi.api.PagedResponse;
import com.gkcontas.openapi.domain.ProductStore;
import com.gkcontas.openapi.error.ApiErrorSchemas;
import com.gkcontas.openapi.error.ProductNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The deprecated version, kept documented.
 *
 * <p>{@code deprecated = true} on the operations is what puts the strikethrough in Swagger
 * UI and the {@code "deprecated": true} flag in the document — which is what a client
 * generator reads to emit a compile-time warning. A migration note in prose that the
 * tooling cannot see reaches nobody.
 */
@RestController
@RequestMapping(value = "/api/v1/products", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Products v1",
        description = "**Deprecated.** Scheduled for removal after 2027-01-01 — migrate to v2.")
public class ProductV1Controller {

    private final ProductStore store;

    public ProductV1Controller(ProductStore store) {
        this.store = store;
    }

    @Operation(
            summary = "List products (deprecated)",
            description = """
                    Returns a flat representation with a single price and a single category.

                    **Migration:** `GET /api/v2/products` returns `price` as an object with `amount`
                    and `currency`, and `categories` as a list. It also distinguishes physical from
                    digital products through the `type` discriminator.
                    """,
            deprecated = true)
    @ApiResponse(responseCode = "200", description = "A page of products")
    @GetMapping
    public PagedResponse<ProductV1Response> list(@ParameterObject @Valid PageQuery query) {
        return new PagedResponse<>(
                store.findAll(query.category(), query.pageOrDefault(), query.sizeOrDefault())
                        .stream().map(ProductV1Response::from).toList(),
                query.pageOrDefault(), query.sizeOrDefault(), store.count(query.category()));
    }

    @Operation(summary = "Fetch one product (deprecated)", deprecated = true)
    @ApiResponse(responseCode = "200", description = "The product")
    @ApiResponse(responseCode = "404", description = "No product with that id",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorSchemas.class)))
    @GetMapping("/{id}")
    public ProductV1Response get(@PathVariable Long id) {
        return store.findById(id).map(ProductV1Response::from)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
