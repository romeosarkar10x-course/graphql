package dev.shelfie.bench;

import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * What a document cache saves: executing the dashboard against a schema whose fields all return null
 * (no data loading at all), with and without a PreparsedDocumentProvider. The difference is the cost of
 * parsing and validating the document on every request.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class DocumentBench {
    String query;
    GraphQL plain, cached;

    @Setup
    public void setup() throws IOException {
        Path contract = Path.of(System.getProperty("contract", "/course/modules/m09-performance/contract"));
        query = Files.readString(contract.resolve("dashboard.graphql"));
        GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(
            new SchemaParser().parse(Files.readString(contract.resolve("club.graphqls"))), RuntimeWiring.newRuntimeWiring().build());
        plain = GraphQL.newGraphQL(schema).build();
        Map<String, PreparsedDocumentEntry> documents = new ConcurrentHashMap<>();
        cached = GraphQL.newGraphQL(schema).preparsedDocumentProvider(new PreparsedDocumentProvider() {
            @Override
            public CompletableFuture<PreparsedDocumentEntry> getDocumentAsync(ExecutionInput input, Function<ExecutionInput, PreparsedDocumentEntry> parseAndValidate) {
                return CompletableFuture.completedFuture(documents.computeIfAbsent(input.getQuery(), q -> parseAndValidate.apply(input)));
            }
        }).build();
    }

    @Benchmark public Object parseEveryTime() { return plain.execute(query).getData(); }

    @Benchmark public Object cachedDocument() { return cached.execute(query).getData(); }
}
