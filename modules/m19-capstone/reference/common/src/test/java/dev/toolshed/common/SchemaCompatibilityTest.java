package dev.toolshed.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.apollographql.federation.graphqljava.Federation;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.introspection.IntrospectionQuery;
import graphql.schema.GraphQLSchema;
import graphql.schema.diff.DiffEvent;
import graphql.schema.diff.DiffLevel;
import graphql.schema.diff.SchemaDiff;
import graphql.schema.diff.SchemaDiffSet;
import graphql.schema.diff.reporting.CapturingReporter;
import graphql.schema.idl.RuntimeWiring;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The schema-diff gate: each subgraph's contract against the released copy (contract/released/), with
 * graphql-java's SchemaDiff on both schemas' introspection. Additions pass; anything breaking fails.
 */
class SchemaCompatibilityTest {
    static final Path CONTRACT = Path.of("../contract");

    @ParameterizedTest
    @ValueSource(strings = {"inventory", "lending"})
    void theContractDoesNotBreakTheReleasedSchema(String subgraph) throws Exception {
        Map<String, Object> released = introspect(CONTRACT.resolve("released/" + subgraph + ".graphqls"));
        Map<String, Object> current = introspect(CONTRACT.resolve(subgraph + ".graphqls"));
        CapturingReporter reporter = new CapturingReporter();
        new SchemaDiff().diffSchema(SchemaDiffSet.diffSetFromIntrospection(released, current), reporter);
        List<String> breaking = reporter.getBreakages().stream().map(DiffEvent::toString).toList();
        assertThat(breaking).as("breaking changes in %s.graphqls", subgraph).isEmpty();
        assertThat(reporter.getEvents()).noneMatch(e -> e.getLevel() == DiffLevel.BREAKING);
    }

    static Map<String, Object> introspect(Path sdl) throws Exception {
        GraphQLSchema schema = Federation.transform(Files.readString(sdl),
                RuntimeWiring.newRuntimeWiring().wiringFactory(new graphql.schema.idl.MockedWiringFactory()).scalar(DateTimeScalar.INSTANCE).build()).fetchEntities(env -> null)
                .resolveEntityType(env -> null).build();
        ExecutionResult r = GraphQL.newGraphQL(schema).build().execute(ExecutionInput.newExecutionInput(IntrospectionQuery.INTROSPECTION_QUERY));
        assertThat(r.getErrors()).isEmpty();
        return r.getData();
    }
}
