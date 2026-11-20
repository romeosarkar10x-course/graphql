import dev.shelfie.writes.Db;
import dev.shelfie.writes.Writes;
import graphql.ExecutionInput;
import graphql.GraphQL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/** Lab 4.4's grader. Prints "pass ..." / "fail ..." lines; exit 1 on any failure. */
public class Harness {
    static int fails = 0;
    static void check(boolean ok, String what, String detail) {
        System.out.println((ok ? "pass " : "fail ") + what + (ok || detail.isEmpty() ? "" : " :: " + detail));
        if (!ok) fails++;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> run(GraphQL g, String q, Map<String, Object> vars) {
        var r = g.execute(ExecutionInput.newExecutionInput(q).variables(vars).build());
        if (!r.getErrors().isEmpty()) throw new IllegalStateException(r.getErrors().toString());
        return (Map<String, Object>) ((Map<String, Object>) r.getData()).values().iterator().next();
    }

    static <T> List<T> concurrently(int n, java.util.function.IntFunction<T> task) throws Exception {
        List<T> out = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < n; i++) {
                int k = i;
                pool.submit(() -> { try { out.add(task.apply(k)); } catch (Throwable t) { errors.add(t); } });
            }
        }
        if (!errors.isEmpty()) throw new IllegalStateException(errors.size() + " request(s) failed, first: " + errors.getFirst());
        return out;
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        String create = "mutation($i: CreateReviewInput!) { createReview(input: $i) { review { id body } replayed userErrors { code } } }";
        String count = "{ reviewCount }";

        // 1. Same key, 50 at once
        GraphQL g = Writes.graphQL(Db.fresh());
        Map<String, Object> input = Map.of("idempotencyKey", "k-1", "bookId", "b1", "rating", 5, "body", "Fifty taps on a flaky train.");
        try {
            List<Map<String, Object>> rs = concurrently(50, i -> run(g, create, Map.of("i", input)));
            int reviews = (Integer) ((Map<String, Object>) g.execute(count).getData()).get("reviewCount");
            check(reviews == 1, "50 concurrent requests with one key create exactly 1 review", "the database has " + reviews);
            long ids = rs.stream().map(r -> String.valueOf(((Map<String, Object>) r.get("review")) == null ? null : ((Map<String, Object>) r.get("review")).get("id"))).distinct().count();
            check(ids == 1 && rs.stream().allMatch(r -> r.get("review") != null), "all 50 responses carry the same review", ids + " distinct review ids (or nulls)");
            long originals = rs.stream().filter(r -> Boolean.FALSE.equals(r.get("replayed"))).count();
            check(originals == 1, "exactly one response has replayed: false", originals + " say replayed: false");
        } catch (Exception e) { check(false, "50 concurrent requests with one key", e.getMessage()); }

        // 2. Sequential retry, and key reuse with another body
        try {
            var again = run(g, create, Map.of("i", input));
            check(Boolean.TRUE.equals(again.get("replayed")), "a later retry with the same key is replayed", String.valueOf(again));
            var reused = run(g, create, Map.of("i", Map.of("idempotencyKey", "k-1", "bookId", "b2", "rating", 1, "body", "A different review entirely.")));
            List<Map<String, Object>> errs = (List<Map<String, Object>>) reused.get("userErrors");
            check(errs != null && !errs.isEmpty() && "KEY_REUSED".equals(errs.getFirst().get("code")), "the same key with a different input is KEY_REUSED", String.valueOf(reused));
            var other = run(g, create, Map.of("i", Map.of("idempotencyKey", "k-2", "bookId", "b2", "rating", 4, "body", "A second, honest review.")));
            check(Boolean.FALSE.equals(other.get("replayed")) && other.get("review") != null, "a new key creates a new review", String.valueOf(other));
        } catch (Exception e) { check(false, "retries and key reuse", e.getMessage()); }

        // 3. Optimistic locking: 50 renames, all read version 0
        GraphQL g2 = Writes.graphQL(Db.fresh());
        String rename = "mutation($i: RenameClubInput!) { renameClub(input: $i) { club { name version } userErrors { code } } }";
        try {
            List<Map<String, Object>> rs = concurrently(50, i -> run(g2, rename, Map.of("i", Map.of("clubId", "c1", "name", "Name " + i, "expectedVersion", 0))));
            long ok = rs.stream().filter(r -> r.get("club") != null).count();
            long conflicts = rs.stream().filter(r -> ((List<Map<String, Object>>) r.get("userErrors")).stream().anyMatch(e -> "CONFLICT".equals(e.get("code")))).count();
            check(ok == 1 && conflicts == 49, "50 renames from version 0: 1 succeeds, 49 get CONFLICT", ok + " succeeded, " + conflicts + " conflicts");
            var club = run(g2, "{ club(id: \"c1\") { version } }", Map.of());
            check(Integer.valueOf(1).equals(club.get("version")), "the version is now 1", String.valueOf(club));
            var next = run(g2, rename, Map.of("i", Map.of("clubId", "c1", "name", "Fresh", "expectedVersion", 1)));
            check(next.get("club") != null, "a rename with the current version succeeds", String.valueOf(next));
        } catch (Exception e) { check(false, "optimistic locking", e.getMessage()); }

        // 4. A job that outlives the request
        GraphQL g3 = Writes.graphQL(Db.fresh());
        try {
            List<String> isbns = List.of("9780441478125", "9781635575637", "nope", "9780679731726", "9780156001311",
                    "9780060853976", "12345", "9780593135204", "9780061054884", "9780385353304");
            long t0 = System.nanoTime();
            var started = run(g3, "mutation($i: ImportBooksInput!) { importBooks(input: $i) { job { id status total imported } } }", Map.of("i", Map.of("isbns", isbns)));
            long ms = (System.nanoTime() - t0) / 1_000_000;
            var job = (Map<String, Object>) started.get("job");
            check(ms < 500 && job != null && !"DONE".equals(job.get("status")), "importBooks answers at once with an unfinished job",
                    "took " + ms + " ms, job " + job);
            String status = "";
            Map<String, Object> seen = Map.of();
            for (int i = 0; i < 60 && !"DONE".equals(status); i++) {
                Thread.sleep(100);
                seen = run(g3, "query($id: ID!) { job(id: $id) { status total imported } }", Map.of("id", job.get("id")));
                status = String.valueOf(seen.get("status"));
            }
            check("DONE".equals(status) && Integer.valueOf(8).equals(seen.get("imported")) && Integer.valueOf(10).equals(seen.get("total")),
                    "polling job(id) reaches DONE with 8 of 10 imported", String.valueOf(seen));
        } catch (Exception e) { check(false, "long-running job", e.getMessage()); }

        System.exit(fails == 0 ? 0 : 1);
    }
}
