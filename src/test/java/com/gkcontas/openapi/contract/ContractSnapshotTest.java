package com.gkcontas.openapi.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.gkcontas.openapi.ApiTestBase;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The contract test: the generated document against a snapshot kept in version control.
 *
 * <p>Nothing else in a Spring application fails when the public contract changes. Rename a
 * field, drop an enum value, tighten a constraint, add a required property — the code
 * compiles, the tests pass, and the consumers find out in production. This test turns any
 * such change into a diff that has to be looked at and committed on purpose.
 *
 * <p>It is not a test of springdoc. It is a test of intent: the snapshot only ever changes
 * when someone decides the contract should change, and reviewing that diff is the moment
 * to ask whether it is backwards compatible.
 *
 * <p>When it fails, the current document is written to {@code build/openapi/<group>.json},
 * so accepting an intentional change is a copy — the failure message says which command.
 */
class ContractSnapshotTest extends ApiTestBase {

    private static final Path SNAPSHOT_DIR = Path.of("src", "test", "resources", "openapi");
    private static final Path ACTUAL_DIR = Path.of("build", "openapi");

    @DisplayName("the generated document still matches the committed snapshot")
    @ParameterizedTest(name = "group {0}")
    @ValueSource(strings = {"public-v1", "public-v2", "admin"})
    void shouldMatchTheCommittedSnapshot(String group) throws Exception {
        JsonNode generated = document(group);
        String pretty = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(generated) + "\n";

        Files.createDirectories(ACTUAL_DIR);
        Path actualFile = ACTUAL_DIR.resolve(group + ".json");
        Files.writeString(actualFile, pretty, StandardCharsets.UTF_8);

        Path snapshotFile = SNAPSHOT_DIR.resolve(group + ".json");
        assertThat(snapshotFile)
                .as("missing snapshot; create it with: cp %s %s", actualFile, snapshotFile)
                .exists();

        String snapshot = Files.readString(snapshotFile, StandardCharsets.UTF_8);
        assertThat(objectMapper.readTree(pretty))
                .as("""
                        The OpenAPI document for group '%s' changed.

                        If the change is intentional, review it for backwards compatibility and \
                        accept it with:
                            cp %s %s
                        """, group, actualFile, snapshotFile)
                .isEqualTo(objectMapper.readTree(snapshot));
    }
}
