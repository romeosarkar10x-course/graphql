package dev.shelfie.schema;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import graphql.language.AstPrinter;
import graphql.language.Document;
import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.EnumTypeDefinition;
import graphql.language.EnumValueDefinition;
import graphql.language.Definition;
import graphql.parser.Parser;
import graphql.schema.diff.DiffEvent;
import graphql.schema.diff.SchemaDiff;
import graphql.schema.diff.SchemaDiffSet;
import graphql.schema.diff.reporting.CapturingReporter;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.UnExecutableSchemaGenerator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** The schema gate: run by `mvn test` locally and in CI on every pull request. */
class SchemaCompatibilityTest {
    static final Path RELEASED = Path.of("released/schema.graphqls");
    static final Path CURRENT = Path.of("src/main/resources/schema.graphqls");

    @Test
    void noBreakingChangesAgainstTheReleasedSchema() throws Exception {
        var reporter = new CapturingReporter();
        new SchemaDiff().diffSchema(SchemaDiffSet.diffSetFromSdl(Files.readString(RELEASED), Files.readString(CURRENT)), reporter);
        // Dangerous changes are allowed, but the build log should show them to the reviewer.
        reporter.getDangers().forEach(e -> System.out.println("DANGEROUS " + e.getReasonMsg()));
        assertTrue(reporter.getBreakages().isEmpty(), () -> "Breaking changes against " + RELEASED + ":\n"
            + reporter.getBreakages().stream().map(DiffEvent::getReasonMsg).collect(Collectors.joining("\n")));
    }

    /** RFC #1231: the schema with every deprecated element removed must itself be valid. */
    @Test
    void stillValidWithDeprecatedElementsRemoved() throws Exception {
        Document doc = Parser.parse(Files.readString(CURRENT));
        List<Definition> kept = doc.getDefinitions().stream().map(d -> switch (d) {
            case ObjectTypeDefinition o -> (Definition) o.transform(b -> b.fieldDefinitions(
                o.getFieldDefinitions().stream().filter(f -> !f.hasDirective("deprecated")).toList()));
            case EnumTypeDefinition e -> e.transform(b -> b.enumValueDefinitions(
                e.getEnumValueDefinitions().stream().filter(v -> !v.hasDirective("deprecated")).toList()));
            default -> d;
        }).toList();
        String stripped = AstPrinter.printAst(doc.transform(b -> b.definitions(kept)));
        assertDoesNotThrow(() -> UnExecutableSchemaGenerator.makeUnExecutableSchema(new SchemaParser().parse(stripped)),
            "removing the deprecated elements would leave an invalid schema");
    }
}
