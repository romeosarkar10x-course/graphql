package dev.shelfie.cms;

import dev.shelfie.cms.Store.Field;
import dev.shelfie.cms.Store.Type;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeRuntimeWiring;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * The whole API is generated: the fixed part (content type management) plus, for every content type, SDL text
 * written from the tables (a type, an input, a patch, two queries, three mutations), parsed and wired. Generating
 * SDL rather than building GraphQLObjectTypes keeps the result readable: print() is what a client would see.
 * A define or remove swaps the GraphQL instance whole (lab 14.7).
 */
@Component
public class CmsSchema implements GraphQlSource {
    private static final Logger log = LoggerFactory.getLogger(CmsSchema.class);
    static final String FIXED = """
        "Every read starts here. Each content type adds a query for one entry and one for a list."
        type Query {
          "Every content type, in the order they were defined."
          contentTypes: [ContentType!]!
        }
        "Each content type adds create, update and delete mutations."
        type Mutation {
          "Create a content type, or redefine one (same name): its API changes at once."
          defineContentType(input: ContentTypeInput!): DefinePayload!
          "Remove a content type and its entries. Refused while another type references it."
          removeContentType(name: String!): RemovePayload!
        }
        "A content type: becomes a GraphQL type with its own queries and mutations."
        type ContentType { name: String! plural: String! description: String fields: [ContentField!]! entryCount: Int! }
        "A field of a content type."
        type ContentField { name: String! kind: FieldKind! ref: String required: Boolean! description: String }
        "What a field holds."
        enum FieldKind { STRING INT FLOAT BOOLEAN REFERENCE }
        "A content type to define."
        input ContentTypeInput { name: String! plural: String! description: String fields: [ContentFieldInput!]! }
        "A field to define. REFERENCE needs ref, the name of a content type (this one, or one already defined)."
        input ContentFieldInput { name: String! kind: FieldKind! ref: String required: Boolean = false description: String }
        "A problem with the input."
        type UserError { field: [String!]! message: String! }
        "The result of defineContentType."
        type DefinePayload { contentType: ContentType userErrors: [UserError!]! }
        "The result of removeContentType."
        type RemovePayload { removed: String userErrors: [UserError!]! }
        """;

    private final Store store;
    private final JsonMapper json;
    private final AtomicReference<GraphQL> current = new AtomicReference<>();
    private Cms cms;

    CmsSchema(Store store, JsonMapper json) {
        this.store = store;
        this.json = json;
    }

    void attach(Cms cms) {
        this.cms = cms;
        rebuild();
    }

    @Override public GraphQL graphQl() { return current.get(); }
    @Override public GraphQLSchema schema() { return current.get().getGraphQLSchema(); }

    static String camel(String name) { return Character.toLowerCase(name.charAt(0)) + name.substring(1); }

    public synchronized void rebuild() {
        List<Type> types = store.types();
        StringBuilder sdl = new StringBuilder(FIXED);
        RuntimeWiring.Builder wiring = RuntimeWiring.newRuntimeWiring();
        TypeRuntimeWiring.Builder query = TypeRuntimeWiring.newTypeWiring("Query")
            .dataFetcher("contentTypes", env -> types.stream().map(this::view).toList());
        TypeRuntimeWiring.Builder mutation = TypeRuntimeWiring.newTypeWiring("Mutation")
            .dataFetcher("defineContentType", env -> cms.define(env.getArgument("input")))
            .dataFetcher("removeContentType", env -> cms.remove(env.getArgument("name")));
        for (Type t : types) {
            String one = camel(t.name()), many = camel(t.plural());
            sdl.append(description(t.description())).append("type ").append(t.name()).append(" {\n")
                .append("  \"Stable identifier.\"\n  id: ID!\n  \"When the entry was created, ISO 8601 UTC.\"\n  createdAt: String!\n");
            StringBuilder input = new StringBuilder(), patch = new StringBuilder();
            TypeRuntimeWiring.Builder fields = TypeRuntimeWiring.newTypeWiring(t.name());
            for (Field f : t.fields()) {
                String scalar = switch (f.kind()) { case "INT" -> "Int"; case "FLOAT" -> "Float"; case "BOOLEAN" -> "Boolean"; case "REFERENCE" -> f.ref(); default -> "String"; };
                String bang = f.required() ? "!" : "";
                sdl.append("  ").append(description(f.description())).append(f.name()).append(": ").append(scalar).append(bang).append('\n');
                String in = f.kind().equals("REFERENCE") ? "ID" : scalar;
                input.append("  ").append(f.name()).append(": ").append(in).append(bang).append('\n');
                patch.append("  ").append(f.name()).append(": ").append(in).append('\n');
                if (f.kind().equals("REFERENCE")) {
                    String ref = f.ref(), name = f.name();
                    fields.dataFetcher(name, env -> {
                        Object id = ((Map<?, ?>) env.getSource()).get(name);
                        return id == null ? null : store.entry(ref, id.toString());
                    });
                }
            }
            sdl.append("}\n");
            if (!input.isEmpty()) {
                sdl.append("\"Fields for a new ").append(t.name()).append(".\"\ninput ").append(t.name()).append("Input {\n").append(input).append("}\n");
                sdl.append("\"Fields to change on a ").append(t.name()).append(": only those given.\"\ninput ").append(t.name()).append("Patch {\n").append(patch).append("}\n");
            }
            sdl.append("extend type Query {\n  \"One ").append(t.name()).append(" by id, or null.\"\n  ").append(one).append("(id: ID!): ").append(t.name())
                .append("\n  \"").append(t.plural()).append(", oldest first.\"\n  ").append(many).append("(first: Int = 20): [").append(t.name()).append("!]!\n}\n");
            if (!input.isEmpty()) {
                sdl.append("extend type Mutation {\n  \"A new ").append(t.name()).append(", or null with a BAD_REQUEST error when a reference points nowhere.\"\n  create").append(t.name()).append("(input: ").append(t.name()).append("Input!): ").append(t.name())
                    .append("\n  \"Change some fields; null when there is no such entry.\"\n  update").append(t.name()).append("(id: ID!, patch: ").append(t.name()).append("Patch!): ").append(t.name())
                    .append("\n  \"Delete an entry; refused while another entry references it. Returns the id, or null.\"\n  delete").append(t.name()).append("(id: ID!): ID\n}\n");
                mutation.dataFetcher("create" + t.name(), env -> result(env, () -> cms.create(t, env.getArgument("input"))))
                    .dataFetcher("update" + t.name(), env -> result(env, () -> cms.update(t, env.getArgument("id"), env.getArgument("patch"))))
                    .dataFetcher("delete" + t.name(), env -> result(env, () -> cms.delete(t, env.getArgument("id"))));
            }
            query.dataFetcher(one, env -> store.entry(t.name(), env.getArgument("id")))
                .dataFetcher(many, env -> store.entries(t.name(), env.getArgument("first")));
            wiring.type(fields);
        }
        wiring.type(query).type(mutation);
        GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(sdl.toString()), wiring.build());
        current.set(GraphQL.newGraphQL(schema).build());
        log.info("schema built with {} content types", types.size());
    }

    Map<String, Object> view(Type t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", t.name());
        m.put("plural", t.plural());
        m.put("description", t.description());
        m.put("fields", t.fields());
        m.put("entryCount", store.count(t.name()));
        return m;
    }

    /** A user's description, written as a GraphQL string: JSON string escaping is valid GraphQL. */
    String description(String text) {
        return text == null ? "" : json.writeValueAsString(text) + "\n  ";
    }

    interface Work { Object run(); }

    /** Rule violations in entry mutations are BAD_REQUEST field errors; the rest of the request goes on. */
    static Object result(DataFetchingEnvironment env, Work work) {
        try {
            return work.run();
        } catch (Cms.Refused e) {
            GraphQLError error = GraphqlErrorBuilder.newError(env).errorType(ErrorType.BAD_REQUEST).message(e.getMessage()).build();
            return DataFetcherResult.newResult().error(error).build();
        }
    }

    static List<String> names(List<Field> fields) {
        List<String> out = new ArrayList<>();
        for (Field f : fields) out.add(f.name());
        return out;
    }
}
