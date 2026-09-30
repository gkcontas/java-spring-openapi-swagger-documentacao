package com.gkcontas.openapi.api;

import com.gkcontas.openapi.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The documented behaviour of the protected API, exercised for real.
 *
 * <p>{@code @SecurityRequirement} only describes the requirement; the filter chain
 * enforces it. Nothing connects the two, so a document can promise a padlock over an
 * endpoint that is wide open. These tests are the connection.
 */
class AdminApiTest extends ApiTestBase {

    private static final String VALID_BODY = """
            {"name": "Test product", "sku": "TP-777", "price": 12.50,
             "kind": "PHYSICAL", "weightKg": 0.5, "categories": ["peripherals"]}
            """;

    @Test
    void shouldRejectAnUnauthenticatedCallWithTheDocumented401() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized())
                // A chain with no login mechanism answers 403 with an empty body by default,
                // which is neither what the annotations say nor the error shape of this API.
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void shouldRejectAWrongToken() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-the-token")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateAProductWithAValidToken() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("PHYSICAL"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.price.currency").value("BRL"));
    }

    @Test
    void shouldRejectASkuThatDoesNotMatchTheDocumentedPattern() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Bad sku", "sku": "nope", "price": 10.00,
                                 "kind": "PHYSICAL", "weightKg": 0.5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("sku must look like AB-123"))
                // The same information again, this time without prose to parse.
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("sku"))
                .andExpect(jsonPath("$.errors[0].message").value("must look like AB-123"));
    }

    @Test
    void shouldRejectADigitalProductWithoutADownloadUrl() throws Exception {
        // The conditional rule the schema cannot express, still enforced.
        mockMvc.perform(post("/api/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Licence", "sku": "LC-001", "price": 99.00, "kind": "DIGITAL"}
                                """))
                .andExpect(status().isBadRequest())
                // A type-level constraint has no field, so the message stands on its own
                // instead of being prefixed with the name of some helper accessor.
                .andExpect(jsonPath("$.detail").value(
                        "weightKg is required when kind is PHYSICAL, and downloadUrl when kind is DIGITAL"))
                .andExpect(jsonPath("$.errors[0].field").doesNotExist());
    }

    @Test
    void shouldReportEveryViolationAtOnceInsteadOfTheFirstOneFound() throws Exception {
        // Validation order is not defined. Returning one arbitrary message sends the caller
        // round the loop once per broken field, and makes assertions pass or fail by luck.
        mockMvc.perform(post("/api/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "X", "sku": "nope", "price": 0.00, "kind": "PHYSICAL"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(4))
                .andExpect(jsonPath("$.errors[*].field").value(
                        org.hamcrest.Matchers.containsInAnyOrder("name", "price", "sku", null)));
    }

    @Test
    void shouldReturn404WhenDeletingAProductThatDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/admin/products/{id}", 9999)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Product not found"));
    }

    @Test
    void shouldDeleteAProductItJustCreated() throws Exception {
        String body = mockMvc.perform(post("/api/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Disposable", "sku": "DP-001", "price": 1.00,
                                 "kind": "PHYSICAL", "weightKg": 0.1}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(delete("/api/admin/products/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldLeaveTheDocumentationEndpointsOpen() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/v3/api-docs/admin"))
                .andExpect(status().isOk());
    }
}
