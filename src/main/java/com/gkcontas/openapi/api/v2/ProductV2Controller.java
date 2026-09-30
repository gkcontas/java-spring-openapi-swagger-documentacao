package com.gkcontas.openapi.api.v2;

import com.gkcontas.openapi.api.PageQuery;
import com.gkcontas.openapi.domain.ProductStore;
import com.gkcontas.openapi.error.ApiErrorSchemas;
import com.gkcontas.openapi.error.ProductNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
 * The current public API.
 *
 * <p>The two named examples on the single-product operation exist because a polymorphic
 * response is exactly the case where one example misleads: a caller who only ever sees
 * the physical shape writes code that breaks the first time a digital product comes back.
 */
@RestController
@RequestMapping(value = "/api/v2/products", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Products v2", description = "The current public catalogue API.")
public class ProductV2Controller {

    private final ProductStore store;

    public ProductV2Controller(ProductStore store) {
        this.store = store;
    }

    @Operation(
            summary = "List products",
            description = """
                    Returns a page of products. Each item carries a `type` discriminator selecting
                    either `PhysicalProductV2` or `DigitalProductV2`.
                    """)
    @ApiResponse(responseCode = "200", description = "A page of products")
    @ApiResponse(responseCode = "400", description = "Invalid paging parameters",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorSchemas.class)))
    @GetMapping
    public ProductV2Page list(@ParameterObject @Valid PageQuery query) {
        return new ProductV2Page(
                store.findAll(query.category(), query.pageOrDefault(), query.sizeOrDefault())
                        .stream().map(ProductV2Response::from).toList(),
                query.pageOrDefault(), query.sizeOrDefault(), store.count(query.category()));
    }

    @Operation(summary = "Fetch one product",
            description = "Inspect the `type` property before reading the shape-specific fields.")
    @ApiResponse(responseCode = "200", description = "The product",
            content = @Content(
                    schema = @Schema(implementation = ProductV2Response.class),
                    examples = {
                            @ExampleObject(name = "physical", summary = "A product that ships",
                                    value = """
                                            {
                                              "type": "PHYSICAL",
                                              "id": 1,
                                              "name": "Mechanical keyboard",
                                              "sku": "KB-001",
                                              "price": {"amount": 450.00, "currency": "BRL"},
                                              "categories": ["peripherals", "input"],
                                              "discontinued": false,
                                              "weightKg": 1.2
                                            }
                                            """),
                            @ExampleObject(name = "digital", summary = "A product delivered by download",
                                    value = """
                                            {
                                              "type": "DIGITAL",
                                              "id": 3,
                                              "name": "Design toolkit licence",
                                              "sku": "SW-100",
                                              "price": {"amount": 990.00, "currency": "BRL"},
                                              "categories": ["software"],
                                              "discontinued": false,
                                              "downloadUrl": "https://example.com/download/sw-100"
                                            }
                                            """)
                    }))
    @ApiResponse(responseCode = "404", description = "No product with that id",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorSchemas.class)))
    @GetMapping("/{id}")
    public ProductV2Response get(
            @Parameter(description = "Catalogue identifier", example = "1", required = true)
            @PathVariable Long id) {
        return store.findById(id).map(ProductV2Response::from)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
