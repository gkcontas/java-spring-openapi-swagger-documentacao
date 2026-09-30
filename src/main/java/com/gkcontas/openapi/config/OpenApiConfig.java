package com.gkcontas.openapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Product Catalogue API")
                        .description("""
                                Catalogue of products, published as two public versions and one
                                administrative API.

                                **v1 is deprecated** and will be removed after 2027-01-01. It returns a
                                single price field and a single category; v2 returns a money object and a
                                list, and distinguishes physical from digital products.
                                """)
                        .version("1.2.0")
                        .contact(new Contact().name("gkcontas").url("https://github.com/gkcontas"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                // Servers are part of the contract: a generated client uses them as the
                // base URL, and leaving them out makes every consumer hard-code one.
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local"),
                        new Server().url("https://api.example.com").description("Production")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("""
                                Paste the token into Swagger UI's **Authorize** dialog. The demo token is
                                `demo-admin-token`.
                                """)));
    }

    /**
     * Public v1: deprecated, still documented.
     *
     * <p>Removing a deprecated version from the documentation is the wrong move — the
     * clients still calling it are exactly the ones who need to find the migration note.
     */
    @Bean
    public GroupedOpenApi publicV1Api() {
        return GroupedOpenApi.builder()
                .group("public-v1")
                .displayName("Public API v1 (deprecated)")
                .pathsToMatch("/api/v1/**")
                .build();
    }

    @Bean
    public GroupedOpenApi publicV2Api() {
        return GroupedOpenApi.builder()
                .group("public-v2")
                .displayName("Public API v2")
                .pathsToMatch("/api/v2/**")
                .build();
    }

    /**
     * Grouping is also a security decision, not only a convenience.
     *
     * <p>Without it, one document lists every endpoint the service has — and an
     * administrative API showing up in the public documentation portal is an invitation
     * that nobody meant to send.
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("Admin API")
                .pathsToMatch("/api/admin/**")
                .build();
    }
}
