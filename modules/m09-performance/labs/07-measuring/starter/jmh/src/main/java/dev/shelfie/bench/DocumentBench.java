package dev.shelfie.bench;

import graphql.GraphQL;
import java.util.concurrent.TimeUnit;
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
 * What a document cache saves. Build two GraphQL instances on contract/club.graphqls (no data fetchers
 * needed), one plain and one with a PreparsedDocumentProvider that caches by query text, and execute
 * contract/dashboard.graphql on each (README).
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
    public void setup() {
        throw new UnsupportedOperationException("build the schema, plain and cached");
    }

    @Benchmark public Object parseEveryTime() { throw new UnsupportedOperationException(); }

    @Benchmark public Object cachedDocument() { throw new UnsupportedOperationException(); }
}
