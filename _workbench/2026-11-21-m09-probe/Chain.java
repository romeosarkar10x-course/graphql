import graphql.*;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatchingContextKeys;
import graphql.schema.idl.*;
import java.util.*;
import java.util.concurrent.*;
import org.dataloader.*;

/** Chained loaders: a fetcher loads from A, then from B with A's result. With and without chaining enabled. */
public class Chain {
    static void sleep() { try { Thread.sleep(20); } catch (InterruptedException e) { throw new RuntimeException(e); } }
    public static void main(String[] args) throws Exception {
        String sdl = "type Query { items: [Item] } type Item { id: Int top: String }";
        var wiring = RuntimeWiring.newRuntimeWiring()
            .type("Query", t -> t.dataFetcher("items", e -> List.of(1, 2, 3, 4)))
            .type("Item", t -> t.dataFetcher("id", e -> e.getSource())
                .dataFetcher("top", e -> {
                    DataLoader<Integer, Integer> a = e.getDataLoader("a");
                    DataLoader<Integer, String> b = e.getDataLoader("b");
                    return a.load(e.<Integer>getSource()).thenCompose(b::load);
                })).build();
        var gql = GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(sdl), wiring)).build();
        for (String mode : List.of("default", "chaining", "exhausted")) {
            List<String> calls = Collections.synchronizedList(new ArrayList<>());
            var reg = DataLoaderRegistry.newRegistry()
                .register("a", DataLoaderFactory.newDataLoader((List<Integer> k) -> { calls.add("a" + k); return CompletableFuture.supplyAsync(() -> { sleep(); return k.stream().map(x -> x * 10).toList(); }); }))
                .register("b", DataLoaderFactory.newDataLoader((List<Integer> k) -> { calls.add("b" + k); return CompletableFuture.supplyAsync(() -> { sleep(); return k.stream().map(x -> "b" + x).toList(); }); }))
                .build();
            var input = ExecutionInput.newExecutionInput("{ items { id top } }").dataLoaderRegistry(reg).build();
            if (mode.equals("chaining")) DataLoaderDispatchingContextKeys.setEnableDataLoaderChaining(input.getGraphQLContext(), true);
            if (mode.equals("exhausted")) DataLoaderDispatchingContextKeys.setEnableDataLoaderExhaustedDispatching(input.getGraphQLContext(), true);
            try {
                var r = gql.executeAsync(input).get(3, TimeUnit.SECONDS);
                System.out.println(mode + ": batches " + calls + " data " + r.getData());
            } catch (TimeoutException e) { System.out.println(mode + ": TIMED OUT after 3 s; batches " + calls); }
        }
        System.exit(0);
    }
}
