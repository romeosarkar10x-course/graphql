package dev.shelfie.writes;

import graphql.ExecutionInput;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * The experiment: 50 virtual threads send the same createReview (one idempotency key) at once,
 * like a client retrying on a flaky network. Prints how many reviews the database ended up with.
 *
 *   java -jar target/shelfie-writes.jar
 */
public final class Main {
    public static void main(String[] args) throws Exception {
        var db = Db.fresh();
        var graphQL = Writes.graphQL(db);
        String m = "mutation($i: CreateReviewInput!) { createReview(input: $i) { review { id } replayed userErrors { code } } }";
        Map<String, Object> input = Map.of("idempotencyKey", "k-1", "bookId", "b1", "rating", 5, "body", "Fifty taps on a flaky train.");
        var ids = ConcurrentHashMap.<String>newKeySet();
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 50; i++) {
                pool.submit(() -> {
                    var r = graphQL.execute(ExecutionInput.newExecutionInput(m).variables(Map.of("i", input)).build());
                    ids.add(String.valueOf(r.getErrors().isEmpty() ? r.getData().toString() : r.getErrors().getFirst().getMessage()));
                });
            }
        }
        var count = graphQL.execute("{ reviewCount }").getData();
        System.out.println("50 concurrent identical requests -> " + count + "; distinct responses: " + ids.size());
        Writes.JOBS.shutdown();
    }
}
