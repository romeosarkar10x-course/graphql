import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lab 4.1: the spec says root mutation fields run serially and query fields may run in parallel.
 * Prove it: three 200 ms fields as a query, then as a mutation, logging start and end per field.
 *
 *   java -cp "$(lib)" SerialVsParallel.java
 * Output: one "<op> <field> start <ms> end <ms> thread <name>" line per field, then "<op> total <ms>".
 */
public class SerialVsParallel {
    static final ExecutorService POOL = Executors.newVirtualThreadPerTaskExecutor();
    static final List<String> LOG = Collections.synchronizedList(new ArrayList<>());
    static long t0;

    /** Async on purpose: graphql-java can only overlap fields whose fetchers return futures. */
    static DataFetcher<CompletableFuture<Integer>> slow(String op) {
        return (DataFetchingEnvironment env) -> CompletableFuture.supplyAsync(() -> {
            long start = ms();
            sleep(200);
            String key = env.getField().getAlias() != null ? env.getField().getAlias() : env.getField().getName();
            LOG.add(String.format("%s %s start %d end %d thread %s", op, key, start, ms(), Thread.currentThread().isVirtual() ? "virtual" : Thread.currentThread().getName()));
            return 1;
        }, POOL);
    }

    static long ms() { return (System.nanoTime() - t0) / 1_000_000; }
    static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { throw new RuntimeException(e); } }

    public static void main(String[] args) {
        var schema = new SchemaGenerator().makeExecutableSchema(
                new SchemaParser().parse("type Query { slow: Int } type Mutation { slow: Int }"),
                RuntimeWiring.newRuntimeWiring()
                        .type("Query", t -> t.dataFetcher("slow", slow("query")))
                        .type("Mutation", t -> t.dataFetcher("slow", slow("mutation")))
                        .build());
        GraphQL graphQL = GraphQL.newGraphQL(schema).build();
        for (String op : List.of("query", "mutation")) {
            LOG.clear();
            t0 = System.nanoTime();
            graphQL.execute(op + " { a: slow b: slow c: slow }");
            long total = ms();
            LOG.stream().sorted().forEach(System.out::println);
            System.out.println(op + " total " + total);
        }
        POOL.shutdown();
    }
}
