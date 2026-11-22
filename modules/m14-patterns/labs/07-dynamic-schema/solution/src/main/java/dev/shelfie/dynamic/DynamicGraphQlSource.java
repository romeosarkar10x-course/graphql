package dev.shelfie.dynamic;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLFloat;
import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;

import dev.shelfie.dynamic.Metadata.FieldDef;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The public schema, built in code from the metadata table, and swapped whole when the metadata changes. Spring's
 * ExecutionGraphQlService asks a GraphQlSource for its GraphQL instance on every request, so a request runs entirely
 * on the schema it started with, and the next one gets the new schema: no restart, no half-built schema.
 *
 * A metadata row that does not make sense (an unknown type, a name GraphQL refuses, a column that does not exist)
 * is skipped with a warning: one bad row must not take the API down.
 */
@Component
public class DynamicGraphQlSource implements GraphQlSource {
    private static final Logger log = LoggerFactory.getLogger(DynamicGraphQlSource.class);
    static final String NAME = "[_A-Za-z][_0-9A-Za-z]*";
    static final Set<String> FIXED = Set.of("id", "title");

    private final Metadata metadata;
    private final AtomicReference<GraphQL> current = new AtomicReference<>();
    private volatile String fingerprint = "";

    DynamicGraphQlSource(Metadata metadata) {
        this.metadata = metadata;
        reload();
    }

    @Override public GraphQL graphQl() { return current.get(); }
    @Override public GraphQLSchema schema() { return current.get().getGraphQLSchema(); }

    /** Every second, and at once after an admin change. Builds only when the fingerprint moved. */
    @Scheduled(fixedDelay = 1000)
    public synchronized void reload() {
        String now = metadata.fingerprint();
        if (now.equals(fingerprint) && current.get() != null) return;
        current.set(GraphQL.newGraphQL(build()).build());
        fingerprint = now;
        log.info("public schema built: Book has {} fields", current.get().getGraphQLSchema().getObjectType("Book").getFieldDefinitions().size());
    }

    GraphQLSchema build() {
        Set<String> columns = metadata.columns();
        GraphQLCodeRegistry.Builder code = GraphQLCodeRegistry.newCodeRegistry();
        GraphQLObjectType.Builder book = GraphQLObjectType.newObject().name("Book").description("A book in the catalogue.")
            .field(f -> f.name("id").type(GraphQLNonNull.nonNull(GraphQLID)).description("Stable identifier, e.g. b1."))
            .field(f -> f.name("title").type(GraphQLNonNull.nonNull(GraphQLString)).description("Title as printed."));
        for (FieldDef def : metadata.fields("Book")) {
            GraphQLOutputType type = switch (def.type()) {
                case "STRING" -> GraphQLString; case "INT" -> GraphQLInt; case "FLOAT" -> GraphQLFloat; case "BOOLEAN" -> GraphQLBoolean;
                default -> null;
            };
            String problem = type == null ? "unknown type " + def.type()
                : !def.name().matches(NAME) || def.name().startsWith("__") ? "not a GraphQL name"
                : FIXED.contains(def.name()) ? "a fixed field"
                : !columns.contains(def.column()) ? "no column m14_07.books." + def.column() : null;
            if (problem != null) {
                log.warn("metadata field Book.{} skipped: {}", def.name(), problem);
                continue;
            }
            book.field(GraphQLFieldDefinition.newFieldDefinition().name(def.name()).type(type).description(def.description()));
            String column = def.column();
            code.dataFetcher(FieldCoordinates.coordinates("Book", def.name()), (DataFetcher<Object>) env -> ((Map<?, ?>) env.getSource()).get(column));
        }
        GraphQLObjectType bookType = book.build();
        GraphQLObjectType query = GraphQLObjectType.newObject().name("Query").description("Every read starts here.")
            .field(f -> f.name("books").type(GraphQLNonNull.nonNull(GraphQLList.list(GraphQLNonNull.nonNull(bookType)))).description("Every book, in id order."))
            .field(f -> f.name("book").type(bookType).description("A book by id, or null.")
                .argument(a -> a.name("id").type(GraphQLNonNull.nonNull(GraphQLID))))
            .build();
        code.dataFetcher(FieldCoordinates.coordinates("Query", "books"), (DataFetcher<Object>) env -> metadata.books());
        code.dataFetcher(FieldCoordinates.coordinates("Query", "book"), (DataFetcher<Object>) env -> metadata.book(env.getArgument("id")));
        return GraphQLSchema.newSchema().query(query).codeRegistry(code.build()).build();
    }
}
