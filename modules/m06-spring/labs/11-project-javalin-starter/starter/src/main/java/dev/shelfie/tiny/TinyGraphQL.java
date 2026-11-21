package dev.shelfie.tiny;

import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import io.javalin.Javalin;
import io.javalin.http.HandlerType;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A tiny GraphQL starter: a schema from the class path, fetchers from annotated methods in a package,
 * GraphQL over HTTP (lab 5.4) on Javalin.
 *
 *   TinyGraphQL.builder().schema("/shelfie.graphqls").scan("dev.shelfie.app")
 *       .context(headers -> Map.of("viewer", headers.get("x-shelfie-user"))).start(4000);
 */
public final class TinyGraphQL {
    private String schema;
    private final List<String> packages = new ArrayList<>();
    private Function<Map<String, String>, Map<String, Object>> context = h -> Map.of();

    private TinyGraphQL() {}

    public static TinyGraphQL builder() { return new TinyGraphQL(); }
    public TinyGraphQL schema(String classpathResource) { this.schema = classpathResource; return this; }
    public TinyGraphQL scan(String pkg) { packages.add(pkg); return this; }
    public TinyGraphQL context(Function<Map<String, String>, Map<String, Object>> fromHeaders) { this.context = fromHeaders; return this; }

    /** Builds the GraphQL object; fails fast when two methods claim one field or a field does not exist. */
    public GraphQL build() {
        String sdl;
        try (var in = TinyGraphQL.class.getResourceAsStream(schema)) {
            if (in == null) throw new IllegalArgumentException("schema not found on the class path: " + schema);
            sdl = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        var types = new SchemaParser().parse(sdl);
        var registry = GraphQLCodeRegistry.newCodeRegistry();
        // TODO: for every @GraphQLController class in `packages` (Scanner), create one instance and register a
        // fetcher (fetcher(...) below) per @Field method at its coordinates. Fail at startup, naming the
        // method, when two methods claim one field or a method names a field the schema does not have.
        var wiring = RuntimeWiring.newRuntimeWiring().codeRegistry(registry).build();
        return GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(types, wiring)).build();
    }

    /** Builds, then serves /graphql on Javalin with virtual threads. */
    public Javalin start(int port) {
        var http = new GraphQLHttp(build(), context);
        return Javalin.create(config -> {
            config.concurrency.useVirtualThreads = true;
            for (var method : List.of(HandlerType.GET, HandlerType.POST, HandlerType.PUT, HandlerType.PATCH, HandlerType.DELETE))
                config.routes.addHttpHandler(method, "/graphql", ctx -> {
                    var headers = new HashMap<String, String>();
                    ctx.headerMap().forEach((k, v) -> headers.put(k.toLowerCase(), v));
                    var r = http.handle(ctx.method().name(), ctx.queryString(), headers, ctx.bodyAsBytes());
                    r.headers().forEach(ctx::header);
                    ctx.status(r.status()).result(r.body());
                });
        }).start(port);
    }

    /** One DataFetcher per method: the parent (non-root types), then @Arg, @Context or the environment. */
    private static DataFetcher<?> fetcher(Object instance, Method m, String type) {
        boolean root = type.equals("Query") || type.equals("Mutation");
        Parameter[] params = m.getParameters();
        return env -> {
            Object[] values = new Object[params.length];
            for (int i = 0; i < params.length; i++) values[i] = bind(params[i], i == 0 && !root, env);
            try {
                return m.invoke(instance, values);
            } catch (InvocationTargetException e) {
                throw e.getCause() instanceof Exception x ? x : e;
            }
        };
    }

    private static Object bind(Parameter p, boolean parent, DataFetchingEnvironment env) {
        if (p.getType() == DataFetchingEnvironment.class) return env;
        if (p.isAnnotationPresent(Context.class)) return env.getGraphQlContext().get(p.getAnnotation(Context.class).value());
        if (p.isAnnotationPresent(Arg.class) || !parent) {
            Arg a = p.getAnnotation(Arg.class);
            return env.getArgument(a == null || a.value().isEmpty() ? p.getName() : a.value());
        }
        return env.getSource();
    }

    private static Object newInstance(Class<?> c) {
        try {
            return c.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(c + " needs a public no-argument constructor", e);
        }
    }
}
