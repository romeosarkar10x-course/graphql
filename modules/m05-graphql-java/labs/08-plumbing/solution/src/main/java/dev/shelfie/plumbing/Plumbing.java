package dev.shelfie.plumbing;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.execution.preparsed.PreparsedDocumentProvider;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

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

    /** Parsed and validated documents, keyed by query text. Bounded: an unbounded cache is a memory DoS. */
    static final class CaffeineDocuments implements PreparsedDocumentProvider {
        private final Cache<String, PreparsedDocumentEntry> cache = Caffeine.newBuilder().maximumSize(1_000).build();

        @Override
        public CompletableFuture<PreparsedDocumentEntry> getDocumentAsync(ExecutionInput input,
                Function<ExecutionInput, PreparsedDocumentEntry> parseAndValidate) {
            // The function parses AND validates. Caching its result skips both next time; safe because this
            // cache belongs to one GraphQL object, so to one schema. Variables are not part of the key.
            return CompletableFuture.completedFuture(cache.get(input.getQuery(), q -> parseAndValidate.apply(input)));
        }
    }

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
            .type("Club", t -> t.dataFetcher("viewerIsMember", env -> {
                // Request-wide values come from GraphQLContext, filled in by whoever built the ExecutionInput.
                String viewer = env.getGraphQlContext().get("viewer");
                return viewer == null ? null : env.<Data.Club>getSource().memberIds().contains(viewer);
            }))
            .type("Book", t -> t.dataFetcher("slowTitle", env -> {
                SLOW_TITLE_CALLS.incrementAndGet();
                Thread.sleep(100);
                return env.<Data.Book>getSource().title();
            }))
            .build();
        var schema = new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(sdl), wiring);
        var builder = GraphQL.newGraphQL(schema).instrumentation(new ParseCounter());
        if (cacheDocuments) builder.preparsedDocumentProvider(new CaffeineDocuments());
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
        var timer = TIMER.schedule(input::cancel, budget.toMillis(), TimeUnit.MILLISECONDS);
        try {
            return graphQL.execute(input);
        } finally {
            timer.cancel(false);
        }
    }

    public static List<String> errorMessages(ExecutionResult r) {
        return r.getErrors().stream().map(GraphQLError::getMessage).toList();
    }
}
