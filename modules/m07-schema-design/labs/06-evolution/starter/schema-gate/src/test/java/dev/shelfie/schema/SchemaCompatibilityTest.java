package dev.shelfie.schema;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The schema gate: run by `mvn test` locally and in CI on every pull request. */
class SchemaCompatibilityTest {
    static final Path RELEASED = Path.of("released/schema.graphqls");
    static final Path CURRENT = Path.of("src/main/resources/schema.graphqls");

    @Test
    void noBreakingChangesAgainstTheReleasedSchema() throws Exception {
        // TODO graphql-java's SchemaDiff (graphql.schema.diff) between RELEASED and CURRENT: fail on any
        // breaking change, naming each; print the dangerous ones.
        fail("TODO");
    }

    /** RFC #1231: the schema with every deprecated element removed must itself be valid. */
    @Test
    void stillValidWithDeprecatedElementsRemoved() throws Exception {
        // TODO parse CURRENT, drop every field and enum value marked @deprecated, print it back to SDL and
        // check that it still builds into a schema.
        fail("TODO");
    }
}
