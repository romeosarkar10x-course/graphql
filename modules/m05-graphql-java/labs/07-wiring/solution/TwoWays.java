import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.introspection.Introspection.DirectiveLocation.FIELD_DEFINITION;

import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLSchemaElement;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphQLTypeVisitorStub;
import graphql.schema.SchemaTransformer;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.SchemaPrinter;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 5.7: one schema, two ways.
 *   sdl | code               print the schema built from small.graphqls / from Java builders
 *   query sdl|code <query>   run a query against either and print the data as JSON
 *   public                   print the code-built schema with every @internal field removed
 *   bench <n>                time building n object types: SDL, newSchema() and FastBuilder
 */
public class TwoWays {
    // The same fetchers serve both schemas: wiring is separate from type definitions.
    @SuppressWarnings("rawtypes") static final Map<String, DataFetcher> QUERY = Map.of(
        "club", env -> Data.club(env.getArgument("id")).orElse(null),
        "clubs", env -> Data.CLUBS);
    @SuppressWarnings("rawtypes") static final Map<String, DataFetcher> CLUB = Map.of(
        "memberCount", env -> env.<Data.Club>getSource().memberIds().size(),
        "owner", env -> Data.user(env.<Data.Club>getSource().ownerId()).orElseThrow());
    @SuppressWarnings("rawtypes") static final Map<String, DataFetcher> USER = Map.of(
        "email", env -> env.<Data.User>getSource().handle() + "@shelfie.example");

    static GraphQLSchema fromSdl() throws Exception {
        var wiring = RuntimeWiring.newRuntimeWiring()
            .type("Query", t -> t.dataFetchers(QUERY))
            .type("Club", t -> t.dataFetchers(CLUB))
            .type("User", t -> t.dataFetchers(USER))
            .build();
        return new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(Files.readString(Path.of("small.graphqls"))), wiring);
    }

    static GraphQLSchema fromCode() {
        var internal = GraphQLDirective.newDirective().name("internal")
            .description("Marks a field that only internal tools may see; the public schema drops it (SchemaTransformer).")
            .validLocation(FIELD_DEFINITION).build();
        var visibility = GraphQLEnumType.newEnum().name("Visibility").value("PUBLIC").value("PRIVATE").build();
        var user = GraphQLObjectType.newObject().name("User")
            .field(f -> f.name("id").type(GraphQLNonNull.nonNull(GraphQLID)))
            .field(f -> f.name("name").type(GraphQLNonNull.nonNull(GraphQLString)))
            .field(f -> f.name("handle").type(GraphQLNonNull.nonNull(GraphQLString)))
            .field(f -> f.name("email").type(GraphQLString).withAppliedDirective(internal.toAppliedDirective()))
            .build();
        var club = GraphQLObjectType.newObject().name("Club")
            .field(f -> f.name("id").type(GraphQLNonNull.nonNull(GraphQLID)))
            .field(f -> f.name("name").type(GraphQLNonNull.nonNull(GraphQLString)))
            .field(f -> f.name("memberCount").type(GraphQLNonNull.nonNull(GraphQLInt)))
            .field(f -> f.name("owner").type(GraphQLNonNull.nonNull(GraphQLTypeReference.typeRef("User"))))
            .field(f -> f.name("visibility").type(GraphQLNonNull.nonNull(visibility)))
            .build();
        var query = GraphQLObjectType.newObject().name("Query")
            .field(f -> f.name("club").type(GraphQLTypeReference.typeRef("Club"))
                .argument(GraphQLArgument.newArgument().name("id").type(GraphQLNonNull.nonNull(GraphQLID))))
            .field(f -> f.name("clubs").type(GraphQLNonNull.nonNull(GraphQLList.list(GraphQLNonNull.nonNull(club)))))
            .build();
        // The code registry holds the fetchers, keyed by coordinates; the types hold none.
        var registry = GraphQLCodeRegistry.newCodeRegistry();
        QUERY.forEach((f, df) -> registry.dataFetcher(FieldCoordinates.coordinates("Query", f), df));
        CLUB.forEach((f, df) -> registry.dataFetcher(FieldCoordinates.coordinates("Club", f), df));
        USER.forEach((f, df) -> registry.dataFetcher(FieldCoordinates.coordinates("User", f), df));
        return GraphQLSchema.newSchema().query(query).additionalType(user).additionalDirective(internal)
            .codeRegistry(registry.build()).build();
    }

    /** A schema transformation: visit every field definition, delete the ones marked @internal. */
    static GraphQLSchema publicView(GraphQLSchema schema) {
        return SchemaTransformer.transformSchema(schema, new GraphQLTypeVisitorStub() {
            @Override
            public TraversalControl visitGraphQLFieldDefinition(GraphQLFieldDefinition field, TraverserContext<GraphQLSchemaElement> ctx) {
                return field.hasAppliedDirective("internal") ? deleteNode(ctx) : TraversalControl.CONTINUE;
            }
        });
    }

    static String print(GraphQLSchema s) {
        return new SchemaPrinter(SchemaPrinter.Options.defaultOptions().includeDirectives(true)).print(s);
    }

    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "sdl" -> System.out.print(print(fromSdl()));
            case "code" -> System.out.print(print(fromCode()));
            case "public" -> System.out.print(print(publicView(fromCode())));
            case "query" -> {
                var schema = args[1].equals("sdl") ? fromSdl() : fromCode();
                var r = GraphQL.newGraphQL(schema).build().execute(args[2]);
                System.out.println(JsonMapper.builder().build().writeValueAsString(r.toSpecification()));
            }
            case "bench" -> bench(Integer.parseInt(args[1]));
            default -> throw new IllegalArgumentException("sdl | code | public | query | bench");
        }
    }

    /**
     * Each T<i> points back at T0, not at T<i+1>: SchemaGenerator recurses once per type along a chain of
     * first references, and a 2,000-type chain overflows the default stack (probe in _workbench).
     */
    static void bench(int n) throws Exception {
        var sdl = new StringBuilder("type Query {\n");
        for (int i = 0; i < n; i++) sdl.append("  t").append(i).append(": T").append(i).append("\n");
        sdl.append("}\n");
        for (int i = 0; i < n; i++)
            sdl.append("type T").append(i).append(" { id: ID! name: String first: T0 }\n");
        for (int round = 0; round < 3; round++) {   // round 0 warms up the JIT; report the last
            long t0 = System.nanoTime();
            var a = new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(sdl.toString()), RuntimeWiring.MOCKED_WIRING);
            long t1 = System.nanoTime();
            var types = new ArrayList<GraphQLObjectType>();
            var q = GraphQLObjectType.newObject().name("Query");
            for (int i = 0; i < n; i++) {
                types.add(GraphQLObjectType.newObject().name("T" + i)
                    .field(f -> f.name("id").type(GraphQLNonNull.nonNull(GraphQLID)))
                    .field(f -> f.name("name").type(GraphQLString))
                    .field(f -> f.name("first").type(GraphQLTypeReference.typeRef("T0"))).build());
                int k = i;
                q.field(f -> f.name("t" + k).type(GraphQLTypeReference.typeRef("T" + k)));
            }
            var query = q.build();
            var b = GraphQLSchema.newSchema().query(query).additionalTypes(new java.util.HashSet<>(types)).build();
            long t2 = System.nanoTime();
            // FastBuilder skips the full traversal and checks newSchema() does; you add every type yourself.
            var fb = new GraphQLSchema.FastBuilder(GraphQLCodeRegistry.newCodeRegistry(), query, null, null);
            types.forEach(fb::addType);
            var c = fb.build();
            long t3 = System.nanoTime();
            if (round == 2) {
                System.out.printf("sdl %d types %.1f ms%n", count(a), (t1 - t0) / 1e6);
                System.out.printf("builder %d types %.1f ms%n", count(b), (t2 - t1) / 1e6);
                System.out.printf("fast %d types %.1f ms%n", count(c), (t3 - t2) / 1e6);
            }
        }
    }

    static long count(GraphQLSchema s) {
        return s.getAllTypesAsList().stream().map(GraphQLNamedType::getName).filter(n -> n.startsWith("T")).count();
    }
}
