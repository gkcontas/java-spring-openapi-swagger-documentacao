package com.gkcontas.openapi.api.v2;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.gkcontas.openapi.domain.Product;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A polymorphic response, described with {@code oneOf} and a discriminator.
 *
 * <p>Without the discriminator the document says only "one of these shapes", and a
 * generated client has no way to decide which one it received — most generators fall back
 * to a union of every field, all optional, which pushes the decision onto the caller at
 * runtime. Naming the property that distinguishes them is what makes the generated code
 * usable.
 *
 * <p>The Jackson annotations and the Swagger ones have to agree: Jackson decides what is
 * serialised, Swagger decides what is documented, and nothing checks that the two match.
 *
 * <p>Hence the explicit {@code discriminatorMapping}. Left out, the generated document
 * carries a discriminator with no mapping, and the specification says that means the
 * value <em>is</em> the schema name. Jackson sends {@code "type": "PHYSICAL"} while the
 * schema is called {@code PhysicalProductV2}, so a client generated from that document
 * looks for a schema named {@code PHYSICAL}, finds nothing, and fails to deserialise a
 * response the service considers perfectly valid. The document is well-formed, passes
 * every linter, and is wrong — which is why {@code ContractIntegrationTest} asserts that
 * the mapping keys are exactly the names Jackson writes.
 */
@Schema(
        name = "ProductV2",
        description = "A catalogue product. The `type` property selects the concrete shape.",
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "PHYSICAL", schema = PhysicalProductV2Response.class),
                @DiscriminatorMapping(value = "DIGITAL", schema = DigitalProductV2Response.class)
        },
        oneOf = {PhysicalProductV2Response.class, DigitalProductV2Response.class})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PhysicalProductV2Response.class, name = "PHYSICAL"),
        @JsonSubTypes.Type(value = DigitalProductV2Response.class, name = "DIGITAL")
})
public sealed interface ProductV2Response
        permits PhysicalProductV2Response, DigitalProductV2Response {

    Long id();

    String name();

    static ProductV2Response from(Product product) {
        return switch (product.kind()) {
            case PHYSICAL -> new PhysicalProductV2Response(product.id(), product.name(), product.sku(),
                    new Money(product.price(), product.currency()), product.categories(),
                    product.discontinued(), product.weightKg());
            case DIGITAL -> new DigitalProductV2Response(product.id(), product.name(), product.sku(),
                    new Money(product.price(), product.currency()), product.categories(),
                    product.discontinued(), product.downloadUrl());
        };
    }
}
