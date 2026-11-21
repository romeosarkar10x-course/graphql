import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Lab 5.3: n sibling fields, each sleeping 200 ms (a stand-in for a blocking JDBC or HTTP call).
 *   java Siblings.java <sync|platform|virtual|mutation> [n=10]
 * Prints "<mode> n=<n> <elapsed> ms"; mutation mode also prints the order the fields ran in.
 */
public class Siblings {
    static final int SLEEP_MS = 200;
    static final List<String> ORDER = new CopyOnWriteArrayList<>();

    static Integer slowCall(String field) {
        try { Thread.sleep(SLEEP_MS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        ORDER.add(field);
        return SLEEP_MS;
    }

    /** sync blocks the calling thread; the async modes hand the blocking call to an executor and return at once. */
    static DataFetcher<?> fetcher(String mode, ExecutorService pool) {
        return switch (mode) {
            case "sync" -> env -> slowCall(env.getField().getName());
            case "platform", "virtual", "mutation" ->
                env -> CompletableFuture.supplyAsync(() -> slowCall(env.getField().getName()), pool);
            default -> throw new IllegalArgumentException("sync | platform | virtual | mutation");
        };
    }

    public static void main(String[] args) {
        String mode = args[0];
        int n = args.length > 1 ? Integer.parseInt(args[1]) : 10;
        String fields = IntStream.range(0, n).mapToObj(i -> "  s" + i + ": Int").collect(Collectors.joining("\n"));
        String sdl = "type Query {\n" + fields + "\n}\ntype Mutation {\n" + fields + "\n}";
        // platform: 4 OS threads, like a small fixed pool. virtual: one cheap virtual thread per task (JDK 21+).
        ExecutorService pool = mode.equals("platform") ? Executors.newFixedThreadPool(4) : Executors.newVirtualThreadPerTaskExecutor();
        DataFetcher<?> df = fetcher(mode, pool);
        var wiring = RuntimeWiring.newRuntimeWiring();
        for (String type : List.of("Query", "Mutation"))
            wiring.type(type, t -> { IntStream.range(0, n).forEach(i -> t.dataFetcher("s" + i, df)); return t; });
        var schema = new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(sdl), wiring.build());
        var graphQL = GraphQL.newGraphQL(schema).build();
        String selection = IntStream.range(0, n).mapToObj(i -> "s" + i).collect(Collectors.joining(" "));
        String query = (mode.equals("mutation") ? "mutation " : "") + "{ " + selection + " }";

        graphQL.execute("{ __typename }");   // warm up the engine, not the fields
        long t0 = System.nanoTime();
        var result = graphQL.execute(ExecutionInput.newExecutionInput(query).build());
        long ms = (System.nanoTime() - t0) / 1_000_000;
        if (!result.getErrors().isEmpty()) throw new IllegalStateException(result.getErrors().toString());
        System.out.println(mode + " n=" + n + " " + ms + " ms");
        if (mode.equals("mutation")) System.out.println("order " + String.join(" ", ORDER));
        pool.shutdownNow();
    }
}
