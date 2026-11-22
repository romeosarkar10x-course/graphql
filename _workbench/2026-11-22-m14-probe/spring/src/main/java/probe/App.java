package probe;

import graphql.ExecutionResult;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.scalars.ExtendedScalars;
import graphql.schema.*;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class App {
    public static void main(String[] args) { SpringApplication.run(App.class, args); }

    record Book(String title, String internalNotes) {}

    static SchemaDirectiveWiring map(String name, java.util.function.BiFunction<String, GraphQLFieldDefinition, String> f) {
        return new SchemaDirectiveWiring() {
            @Override public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> env) {
                var field = env.getElement();
                var coords = FieldCoordinates.coordinates(env.getFieldsContainer(), field);
                var original = env.getCodeRegistry().getDataFetcher(coords, field);
                System.out.println("WIRING " + name + " on " + field.getName());
                DataFetcher<?> wrapped = e -> {
                    Object v = original.get(e);
                    String out = v == null ? null : f.apply(v.toString(), field);
                    System.out.println("RUN " + name + " on " + field.getName() + ": " + v + " -> " + out);
                    return out;
                };
                env.getCodeRegistry().dataFetcher(coords, wrapped);
                return field;
            }
        };
    }

    @Bean RuntimeWiringConfigurer wiring() {
        return b -> b.scalar(ExtendedScalars.DateTime)
            .directive("uppercase", map("uppercase", (v, f) -> v.toUpperCase()))
            .directive("mask", map("mask", (v, f) -> v.substring(0, 2) + "*".repeat(Math.max(0, v.length() - 2))));
    }

    // Per-request schema: hide Query.secret and Book.internalNotes unless X-Tenant: staff
    @Bean WebGraphQlInterceptor tenant() {
        return (req, chain) -> {
            String t = req.getHeaders().getFirst("X-Tenant");
            req.configureExecutionInput((in, b) -> b.graphQLContext(Map.of("tenant", t == null ? "public" : t)).build());
            return chain.next(req);
        };
    }

    @Bean Instrumentation perTenantSchema() {
        return new Instrumentation() {
            @Override public GraphQLSchema instrumentSchema(GraphQLSchema schema, InstrumentationExecutionParameters p, InstrumentationState s) {
                String t = p.getGraphQLContext().get("tenant");
                System.out.println("instrumentSchema tenant=" + t);
                if ("staff".equals(t)) return schema;
                return SchemaTransformer.transformSchema(schema, new GraphQLTypeVisitorStub() {
                    @Override public graphql.util.TraversalControl visitGraphQLFieldDefinition(GraphQLFieldDefinition node, graphql.util.TraverserContext<GraphQLSchemaElement> ctx) {
                        if (node.getName().equals("secret") || node.getName().equals("internalNotes")) return deleteNode(ctx);
                        return graphql.util.TraversalControl.CONTINUE;
                    }
                });
            }
        };
    }

    @Controller static class Q {
        @QueryMapping String a() { return "piranesi"; }
        @QueryMapping String b() { return "piranesi"; }
        @QueryMapping String locale(Locale locale) { return String.valueOf(locale); }
        @QueryMapping OffsetDateTime now() { return OffsetDateTime.of(2026, 11, 22, 12, 0, 0, 0, ZoneOffset.UTC); }
        @QueryMapping String secret() { return "s3cret"; }
        @QueryMapping Book book() { return new Book("Piranesi", "staff only"); }
    }
}
