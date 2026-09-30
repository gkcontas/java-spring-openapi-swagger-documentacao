package com.gkcontas.openapi.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.gkcontas.openapi.ApiTestBase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assertions on the shape of the document rather than on its exact bytes.
 *
 * <p>These complement the snapshot. The snapshot catches <em>any</em> change; these catch
 * the specific mistakes that produce a document which looks fine and misleads anyway — and
 * they keep working after the snapshot is legitimately updated.
 */
class DocumentStructureTest extends ApiTestBase {

    @Test
    void shouldKeepTheAdminApiOutOfThePublicDocuments() throws Exception {
        for (String group : List.of("public-v1", "public-v2")) {
            assertThat(document(group).get("paths").fieldNames())
                    .toIterable()
                    .as("group %s must not expose administrative paths", group)
                    .noneMatch(path -> path.startsWith("/api/admin"));
        }
        assertThat(document("admin").get("paths").fieldNames()).toIterable()
                .allMatch(path -> path.startsWith("/api/admin"));
    }

    @Test
    void shouldNotLeakV1PathsIntoV2() throws Exception {
        assertThat(document("public-v2").get("paths").fieldNames()).toIterable()
                .allMatch(path -> path.startsWith("/api/v2"));
    }

    @Test
    void shouldFlagEveryV1OperationAsDeprecated() throws Exception {
        JsonNode paths = document("public-v1").get("paths");
        assertThat(paths).isNotEmpty();
        paths.forEach(operations -> operations.forEach(operation ->
                assertThat(operation.path("deprecated").asBoolean())
                        .as("operation '%s' must carry deprecated: true so generators warn",
                                operation.path("summary").asText())
                        .isTrue()));
    }

    @Test
    void shouldDescribeTheDiscriminatorWithTheValuesJacksonActuallyWrites() throws Exception {
        JsonNode discriminator = document("public-v2")
                .at("/components/schemas/ProductV2/discriminator");

        assertThat(discriminator.get("propertyName").asText()).isEqualTo("type");
        // Without an explicit mapping the specification says the value IS the schema name,
        // so a client generated from the document would look for a schema called
        // "PHYSICAL". Jackson writes "PHYSICAL"; the schema is called PhysicalProductV2.
        assertThat(discriminator.get("mapping").fieldNames()).toIterable()
                .containsExactlyInAnyOrder("PHYSICAL", "DIGITAL");
        assertThat(discriminator.at("/mapping/PHYSICAL").asText())
                .isEqualTo("#/components/schemas/PhysicalProductV2");
    }

    @Test
    void shouldReferenceTheErrorSchemaInsteadOfInliningItEverywhere() throws Exception {
        JsonNode v2 = document("public-v2");
        JsonNode notFound = v2.at("/paths/~1api~1v2~1products~1{id}/get/responses/404");

        // One shared $ref, not a copy per operation: a generated client then has a single
        // error type instead of one anonymous class per endpoint.
        assertThat(notFound.at("/content/application~1problem+json/schema/$ref").asText())
                .isEqualTo("#/components/schemas/ProblemDetail");
        assertThat(v2.at("/components/schemas/ProblemDetail")).isNotEmpty();
    }

    @Test
    void shouldTurnBeanValidationConstraintsIntoSchemaConstraints() throws Exception {
        JsonNode request = document("admin").at("/components/schemas/CreateProductRequest");

        assertThat(request.at("/properties/sku/pattern").asText()).isEqualTo("^[A-Z]{2}-\\d{3}$");
        assertThat(request.at("/properties/name/minLength").asInt()).isEqualTo(2);
        assertThat(request.at("/properties/name/maxLength").asInt()).isEqualTo(120);
        assertThat(request.at("/properties/price/minimum").decimalValue())
                .isEqualByComparingTo("0.01");

        List<String> required = new ArrayList<>();
        request.get("required").forEach(node -> required.add(node.asText()));
        assertThat(required).containsExactlyInAnyOrder("name", "sku", "price", "kind");
    }

    @Test
    void shouldExposeExactlyTheRequestFieldsAndNothingElse() throws Exception {
        // Guards against a validator leaking into the contract. The cross-field rule here
        // is a type-level constraint precisely so that it has no accessor: an @AssertTrue
        // helper would be read by Jackson as a getter and appear in this list as a
        // "deliveryDetailValid" boolean no caller can send.
        assertThat(document("admin").at("/components/schemas/CreateProductRequest/properties")
                .fieldNames()).toIterable()
                .containsExactlyInAnyOrder("name", "sku", "price", "currency",
                        "categories", "kind", "weightKg", "downloadUrl");
    }

    @Test
    void shouldDocumentTheValidationErrorExtensionOfProblemDetail() throws Exception {
        JsonNode problem = document("admin").at("/components/schemas/ProblemDetail");

        // RFC 7807 allows extension members, and a caller can only rely on one that is in
        // the document. An undocumented extension is indistinguishable from a bug.
        assertThat(problem.at("/properties/errors/items/$ref").asText())
                .isEqualTo("#/components/schemas/ValidationError");
    }

    @Test
    void shouldFlattenTheParameterObjectIntoQueryParameters() throws Exception {
        JsonNode parameters = document("public-v2").at("/paths/~1api~1v2~1products/get/parameters");

        List<String> names = new ArrayList<>();
        parameters.forEach(parameter -> names.add(parameter.get("name").asText()));
        // Not a single "pageQuery" object parameter: @ParameterObject expands the record.
        assertThat(names).containsExactlyInAnyOrder("page", "size", "category");
        assertThat(parameters.get(0).get("in").asText()).isEqualTo("query");
    }

    @Test
    void shouldCarryBothPolymorphicExamplesOnTheSingleProductOperation() throws Exception {
        JsonNode examples = document("public-v2")
                .at("/paths/~1api~1v2~1products~1{id}/get/responses/200/content/application~1json/examples");

        // One example on a polymorphic response teaches callers the wrong shape.
        assertThat(examples.fieldNames()).toIterable().containsExactlyInAnyOrder("physical", "digital");
        assertThat(examples.at("/digital/value/downloadUrl").asText()).isNotBlank();
    }

    @Test
    void shouldDocumentTheMediaTypesTheServiceReallySends() throws Exception {
        JsonNode v2 = document("public-v2");

        // Without `produces` on the mapping, springdoc writes `*/*` — which tells a
        // generated client nothing and quietly disagrees with the `application/json` the
        // service sends. The error path sends `application/problem+json`, a different
        // media type again, and the annotation has to say so.
        assertThat(v2.at("/paths/~1api~1v2~1products~1{id}/get/responses/200/content")
                .fieldNames()).toIterable().containsExactly("application/json");
        assertThat(v2.at("/paths/~1api~1v2~1products~1{id}/get/responses/404/content")
                .fieldNames()).toIterable().containsExactly("application/problem+json");
    }

    @Test
    void shouldDeclareTheBearerSchemeAndRequireItOnAdminOperations() throws Exception {
        JsonNode admin = document("admin");

        assertThat(admin.at("/components/securitySchemes/bearerAuth/scheme").asText())
                .isEqualTo("bearer");
        admin.get("paths").forEach(operations -> operations.forEach(operation ->
                assertThat(operation.at("/security/0/bearerAuth"))
                        .as("every admin operation must be marked as protected")
                        .isNotNull()));
    }

    @Test
    void shouldServeTheGroupListConsumedBySwaggerUi() throws Exception {
        JsonNode config = objectMapper.readTree(mockMvc
                .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/v3/api-docs/swagger-config"))
                .andReturn().getResponse().getContentAsString());

        List<String> names = new ArrayList<>();
        config.get("urls").forEach(url -> names.add(url.get("name").asText()));
        assertThat(names).containsExactlyInAnyOrder(
                "Public API v1 (deprecated)", "Public API v2", "Admin API");
    }
}
