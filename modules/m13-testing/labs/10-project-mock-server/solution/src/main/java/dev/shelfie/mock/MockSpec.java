package dev.shelfie.mock;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.TypeResolutionEnvironment;
import graphql.language.Directive;
import graphql.language.Document;
import graphql.language.OperationDefinition;
import graphql.language.StringValue;
import graphql.parser.Parser;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLSchemaElement;
import graphql.schema.GraphQLTypeVisitorStub;
import graphql.schema.SchemaTransformer;
import graphql.schema.TypeResolver;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLUnionType;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.SerializationFeature;

/**
 * MockSpec (RFC 0, strawman, graphql-wg rfcs/MockSpec.md), read server-side: the spec is written for clients, so
 * a client in any language can point at this server and get the same mock files a JavaScript client would read.
 *
 *  - An operation carrying @mock or @mock(name: "x") is answered from <mocks>/<OperationName>.json: the "__default__"
 *    entry, or the named one ("__description__" keys are dropped from the answer).
 *  - A missing name is an error that lists the available names (ResolveMockedOperation).
 *  - A missing file is generated from the fake data and written as {"__default__": ...} (GenerateDefaultMockFile).
 *  - A mock is checked against the operation before it is served, the way the spec suggests: execute the operation
 *    with no resolvers and the mock's data as the root value; any error means the mock no longer fits.
 */
final class MockSpec {

    private final Path dir;
    private final GraphQL replay;
    private final JsonMapper json = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    MockSpec(GraphQLSchema schema, Path dir) {
        this.dir = dir;
        this.replay = GraphQL.newGraphQL(replaying(schema)).build();
    }

    /** Is this an operation with @mock? Parse errors are left to normal execution. */
    boolean handles(String query, String operationName) {
        try { return operation(Parser.parse(query), operationName).flatMap(MockSpec::mockDirective).isPresent(); }
        catch (Exception e) { return false; }
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> respond(GraphQL graphql, ExecutionInput input) {
        Document doc = Parser.parse(input.getQuery());
        OperationDefinition op = operation(doc, input.getOperationName()).orElseThrow();
        Directive mock = mockDirective(op).orElseThrow();
        String name = mock.getArgument("name") != null && mock.getArgument("name").getValue() instanceof StringValue s ? s.getValue() : "__default__";
        if (op.getName() == null) return error("MockSpec needs a named operation: mock files are named after it");
        if (dir == null) return error("Start the server with --mocks <dir> to answer @mock operations");
        Path file = dir.resolve(op.getName() + ".json");
        try {
            if (!Files.exists(file)) {
                if (!name.equals("__default__")) return error("No mock file " + file.getFileName() + " for " + op.getName() + ", so no mock named \"" + name + "\"");
                Map<String, Object> generated = graphql.execute(input).toSpecification();
                Files.createDirectories(dir);
                Files.writeString(file, json.writeValueAsString(Map.of("__default__", generated)) + "\n");
                return generated;
            }
            Map<String, Object> mocks = json.readValue(Files.readString(file), LinkedHashMap.class);
            if (!mocks.containsKey(name))
                return error("No mock named \"" + name + "\" for " + op.getName() + "; available: "
                    + String.join(", ", mocks.keySet().stream().filter(k -> !k.equals("__description__")).toList()));
            Map<String, Object> response = new LinkedHashMap<>((Map<String, Object>) mocks.get(name));
            response.remove("__description__");
            if (!response.containsKey("data")) return error("Mock \"" + name + "\" for " + op.getName() + " has no \"data\" key: a mock response is a GraphQL response");
            String misfit = misfit(input, response);
            if (misfit != null) return error("Mock \"" + name + "\" for " + op.getName() + " does not match the operation: " + misfit);
            return response;
        } catch (IOException | RuntimeException e) {
            return error("Mock file " + file.getFileName() + ": " + e.getMessage());
        }
    }

    /** The spec's suggestion: run the operation with the mock's data as the root value and no resolvers. */
    @SuppressWarnings("unchecked")
    String misfit(ExecutionInput input, Map<String, Object> response) {
        Object data = response.get("data");
        if (data == null) return null;   // "data": null is a valid response (with errors, usually)
        ExecutionResult r = replay.execute(input.transform(b -> b.root(data)));
        if (!r.getErrors().isEmpty()) return r.getErrors().getFirst().getMessage();
        return null;
    }

    static Optional<OperationDefinition> operation(Document doc, String name) {
        var ops = doc.getDefinitionsOfType(OperationDefinition.class);
        if (name == null) return ops.size() == 1 ? Optional.of(ops.getFirst()) : Optional.empty();
        return ops.stream().filter(o -> name.equals(o.getName())).findFirst();
    }

    static Optional<Directive> mockDirective(OperationDefinition op) {
        return op.getDirectives().stream().filter(d -> d.getName().equals("mock")).findFirst();
    }

    /** A GraphQL response with "data": null and one error. */
    static Map<String, Object> error(String message) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("errors", List.of(Map.of("message", message)));
        r.put("data", null);
        return r;
    }

    /** The same schema, but every field reads its response key from the parent map and types come from __typename. */
    static GraphQLSchema replaying(GraphQLSchema schema) {
        DataFetcher<Object> byKey = env -> env.getSource() instanceof Map<?, ?> m ? m.get(env.getField().getResultKey()) : null;
        TypeResolver byTypename = (TypeResolutionEnvironment env) -> env.getObject() instanceof Map<?, ?> m && m.get("__typename") instanceof String t
            ? env.getSchema().getObjectType(t) : null;
        var code = GraphQLCodeRegistry.newCodeRegistry(schema.getCodeRegistry()).clearDataFetchers().defaultDataFetcher(graphql.schema.DataFetcherFactories.useDataFetcher(byKey));
        for (var t : schema.getAllTypesAsList()) {
            if (t instanceof GraphQLInterfaceType i) code.typeResolver(i, byTypename);
            if (t instanceof GraphQLUnionType u) code.typeResolver(u, byTypename);
        }
        return schema.transform(b -> b.codeRegistry(code.build()));
    }
}
