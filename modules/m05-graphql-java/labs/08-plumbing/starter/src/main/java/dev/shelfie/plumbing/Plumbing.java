package dev.shelfie.plumbing;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.language.Document;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/** Lab 5.8: the plumbing around execution. The test harness calls these methods; keep their signatures. */
public final class Plumbing {
    private Plumbing() {}

    private static final AtomicInteger PARSES = new AtomicInteger();
    private static final AtomicInteger SLOW_TITLE_CALLS = new AtomicInteger();
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        var t = new Thread(r, "cancel-timer");
        t.setDaemon(true);
        return t;
    });

    /** How many times graphql-java has parsed a document, across all GraphQL instances. */
    public static int parseCount() { return PARSES.get(); }

    /** How many times Book.slowTitle's fetcher has started. */
    public static int slowTitleCalls() { return SLOW_TITLE_CALLS.get(); }

    // TODO: a PreparsedDocumentProvider backed by a bounded Caffeine cache (Caffeine is in pom.xml).

    /** Counts parses, so the test can see the cache work. */
    static final class ParseCounter extends SimplePerformantInstrumentation {
        @Override
        public InstrumentationContext<Document> beginParse(InstrumentationExecutionParameters p, InstrumentationState s) {
            PARSES.incrementAndGet();
            return super.beginParse(p, s);
        }
    }

    public static GraphQL graphQL(boolean cacheDocuments) {
        String sdl;
        try (var in = Plumbing.class.getResourceAsStream("/plumbing.graphqls")) {
            sdl = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        var wiring = RuntimeWiring.newRuntimeWiring()
            .type("Query", t -> t
                .dataFetcher("clubs", env -> Data.CLUBS)
                .dataFetcher("books", env -> Data.BOOKS))
            // TODO: true/false from the viewer in GraphQLContext; null when there is none
            .type("Club", t -> t.dataFetcher("viewerIsMember", env -> null))
            .type("Book", t -> t.dataFetcher("slowTitle", env -> {
                SLOW_TITLE_CALLS.incrementAndGet();
                Thread.sleep(100);
                return env.<Data.Book>getSource().title();
            }))
            .build();
        var schema = new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(sdl), wiring);
        var builder = GraphQL.newGraphQL(schema).instrumentation(new ParseCounter());
        // TODO: when cacheDocuments, plug your provider in
        return builder.build();
    }

    /** An input whose GraphQLContext carries the viewer (null: nobody signed in). */
    public static ExecutionInput input(String query, String viewer) {
        var b = ExecutionInput.newExecutionInput(query);
        if (viewer != null) b.graphQLContext(Map.of("viewer", viewer));
        return b.build();
    }

    /** Executes, and cancels the execution if it is still running after `budget`. */
    public static ExecutionResult executeWithin(GraphQL graphQL, ExecutionInput input, Duration budget) {
        // TODO: stop the execution when the budget runs out (TIMER is there for you)
        return graphQL.execute(input);
    }

    public static List<String> errorMessages(ExecutionResult r) {
        return r.getErrors().stream().map(GraphQLError::getMessage).toList();
    }
}
