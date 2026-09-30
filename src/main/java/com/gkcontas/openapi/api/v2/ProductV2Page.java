package com.gkcontas.openapi.api.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * A page of products, with {@code content} typed concretely instead of generically.
 *
 * <p>This exists because of a trap that produces a document telling the truth about a
 * response the service does not send.
 *
 * <p>Returning {@code PagedResponse<ProductV2Response>} works for the schema — springdoc
 * reads the method's generic return type and correctly writes {@code items: {$ref:
 * ProductV2}}, discriminator and all. At runtime it does not. Spring hands Jackson the
 * generic return type, but {@code AbstractJackson2HttpMessageConverter} only calls
 * {@code ObjectWriter.forType(...)} when that type {@code isContainerType()} — true for a
 * {@code List}, false for a record that merely contains one. So Jackson serialises the
 * wrapper by its runtime class, where {@code content} is declared {@code List<T>} with
 * {@code T} erased to {@code Object}, and a type serializer is only applied when the
 * declared type carries {@code @JsonTypeInfo}. The {@code type} property silently
 * disappears — from the list endpoint only. Fetching one product by id keeps it, because
 * there the declared return type <em>is</em> {@code ProductV2Response}.
 *
 * <p>The result is the worst kind of contract bug: the document is right, the single-item
 * endpoint is right, and the list endpoint returns items a generated client cannot
 * deserialise. Naming the element type here removes the erasure and the type serializer
 * applies again. {@code PublicApiTest} pins both endpoints so it cannot come back.
 *
 * <p>{@code PagedResponse<T>} is still used by v1, where the items are a plain record with
 * no polymorphism and erasure costs nothing.
 */
@Schema(name = "PagedProductV2", description = "A page of products")
public record ProductV2Page(

        @Schema(description = "The products on this page")
        List<ProductV2Response> content,

        @Schema(description = "Zero-based index of this page", example = "0")
        int page,

        @Schema(description = "Requested page size", example = "20")
        int size,

        @Schema(description = "Total number of matching products across all pages", example = "42")
        long totalElements) {
}
