package com.gkcontas.openapi.api;

import com.gkcontas.openapi.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicApiTest extends ApiTestBase {

    @Test
    void shouldServeTheFlatRepresentationOnV1() throws Exception {
        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(450.00))
                .andExpect(jsonPath("$.category").value("peripherals"))
                // v1 knows nothing about the discriminator or the money object.
                .andExpect(jsonPath("$.type").doesNotExist())
                .andExpect(jsonPath("$.categories").doesNotExist());
    }

    @Test
    void shouldServeTheDiscriminatedRepresentationOnV2() throws Exception {
        mockMvc.perform(get("/api/v2/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("PHYSICAL"))
                .andExpect(jsonPath("$.price.amount").value(450.00))
                .andExpect(jsonPath("$.price.currency").value("BRL"))
                .andExpect(jsonPath("$.categories.length()").value(2))
                .andExpect(jsonPath("$.weightKg").value(1.2));
    }

    @Test
    void shouldUseTheDigitalShapeForADownloadableProduct() throws Exception {
        mockMvc.perform(get("/api/v2/products/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DIGITAL"))
                .andExpect(jsonPath("$.downloadUrl").value("https://example.com/download/sw-100"))
                .andExpect(jsonPath("$.weightKg").doesNotExist());
    }

    @Test
    void shouldReturnProblemDetailForAnUnknownProduct() throws Exception {
        mockMvc.perform(get("/api/v2/products/9999"))
                .andExpect(status().isNotFound())
                // Exactly the shape the document promises under 404.
                .andExpect(jsonPath("$.title").value("Product not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").value("/api/v2/products/9999"));
    }

    @Test
    void shouldApplyTheDefaultsTheSchemaAdvertises() throws Exception {
        mockMvc.perform(get("/api/v2/products"))
                .andExpect(status().isOk())
                // The schema says page defaults to 0 and size to 20; if the code disagreed,
                // the document would be describing an API that does not exist.
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    void shouldRejectAPageSizeAboveTheDocumentedMaximum() throws Exception {
        mockMvc.perform(get("/api/v2/products").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").value("size must be at most 100"));
    }

    @Test
    void shouldKeepTheDiscriminatorOnItemsInsideAList() throws Exception {
        // The regression this guards: a generic page wrapper erases its element type
        // during serialisation, and the `type` property vanishes from list items while the
        // single-item endpoint keeps it and the document keeps promising it.
        // ProductV2Page has the mechanism in full.
        mockMvc.perform(get("/api/v2/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("PHYSICAL"))
                .andExpect(jsonPath("$.content[2].type").value("DIGITAL"));
    }

    @Test
    void shouldSendTheMediaTypesTheDocumentAdvertises() throws Exception {
        mockMvc.perform(get("/api/v2/products/1"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        mockMvc.perform(get("/api/v2/products/9999"))
                .andExpect(status().isNotFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void shouldFilterByCategory() throws Exception {
        mockMvc.perform(get("/api/v2/products").param("category", "software"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("DIGITAL"));
    }
}
