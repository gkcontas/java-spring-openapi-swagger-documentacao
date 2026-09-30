package com.gkcontas.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkcontas.openapi.domain.ProductStore;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * One context for every test class in the suite.
 *
 * <p>The annotations live here and nowhere else, so Spring caches a single application
 * context: the controllers, the security chain and springdoc are all built once. Spreading
 * the same annotations across classes with slightly different attributes is the usual
 * reason a small suite takes a minute.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class ApiTestBase {

    protected static final String ADMIN_TOKEN = "demo-admin-token";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private ProductStore store;

    @BeforeEach
    void resetCatalogue() {
        store.reset();
    }

    /** The generated document for one springdoc group, as JSON. */
    protected JsonNode document(String group) throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs/{group}", group))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }
}
