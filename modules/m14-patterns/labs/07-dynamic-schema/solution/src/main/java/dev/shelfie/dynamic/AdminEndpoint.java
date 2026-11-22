package dev.shelfie.dynamic;

import dev.shelfie.dynamic.Metadata.FieldDef;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.execution.DefaultExecutionGraphQlService;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * A second GraphQL endpoint in the same JVM, set up by hand the way Spring's auto-configuration sets up the first:
 * a GraphQlSource (its own schema file and wiring), an ExecutionGraphQlService, a WebGraphQlHandler and an HTTP
 * handler, routed at /admin/graphql. It is not a bean: the auto-configuration keeps serving /graphql from the
 * DynamicGraphQlSource bean.
 */
@Configuration
public class AdminEndpoint {
    public record UserError(String message) {}
    public record FieldPayload(FieldDef field, List<UserError> userErrors) {}

    @Bean
    RouterFunction<ServerResponse> adminRoutes(Metadata metadata, DynamicGraphQlSource publicSchema, @Value("${shelfie.admin-key}") String key) {
        GraphQlSource admin = GraphQlSource.schemaResourceBuilder()
            .schemaResources(new ClassPathResource("admin/admin.graphqls"))
            .configureRuntimeWiring(wiring -> wiring
                .type("Query", t -> t.dataFetcher("fields", env -> metadata.fields(env.getArgument("typeName"))))
                .type("FieldDef", t -> t.dataFetcher("column", env -> ((FieldDef) env.getSource()).column()))
                .type("Mutation", t -> t
                    .dataFetcher("addField", env -> add(metadata, publicSchema, env.getArgument("typeName"), env.getArgument("name"),
                        env.getArgument("type"), env.getArgument("description")))
                    .dataFetcher("removeField", env -> {
                        FieldDef removed = metadata.remove(env.getArgument("typeName"), env.getArgument("name"));
                        if (removed == null) return new FieldPayload(null, List.of(new UserError("No field " + env.getArgument("name"))));
                        publicSchema.reload();
                        return new FieldPayload(removed, List.of());
                    })))
            .build();
        GraphQlHttpHandler http = new GraphQlHttpHandler(WebGraphQlHandler.builder(new DefaultExecutionGraphQlService(admin)).build());
        return RouterFunctions.route()
            .POST("/admin/graphql", request -> key.equals(request.headers().firstHeader("X-Admin-Key"))
                ? http.handleRequest(request)
                : ServerResponse.status(HttpStatus.UNAUTHORIZED).body(Map.of("errors", List.of(Map.of("message", "X-Admin-Key required")))))
            .build();
    }

    static FieldPayload add(Metadata metadata, DynamicGraphQlSource publicSchema, String typeName, String name, String type, String description) {
        if (!"Book".equals(typeName)) return new FieldPayload(null, List.of(new UserError("Only Book has metadata fields")));
        if (!name.matches(DynamicGraphQlSource.NAME) || name.startsWith("__"))
            return new FieldPayload(null, List.of(new UserError("'" + name + "' is not a GraphQL name: letters, digits and _, not starting with a digit or __")));
        if (DynamicGraphQlSource.FIXED.contains(name) || metadata.fields(typeName).stream().anyMatch(f -> f.name().equals(name)))
            return new FieldPayload(null, List.of(new UserError("Book already has a field " + name)));
        FieldDef def = metadata.add(typeName, name, type, description);
        publicSchema.reload();
        return new FieldPayload(def, List.of());
    }
}
