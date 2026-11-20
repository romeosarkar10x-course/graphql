import graphql.GraphQL;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Game 4a: points that survive the chaos button. Every call to graphQL() is a fresh service:
 * users u1..u8 with 100 points; rewards "bookmark" (30 points, 5 in stock) and "tote" (80 points, 2).
 *
 * This first draft passes every test you would write by hand, one request at a time. Run the chaos
 * button (README) and make it survive: retries must not double-apply, races must not lose updates.
 */
public final class Points {
    private Points() {}

    static final class Service {
        final Map<String, Integer> balances = new ConcurrentHashMap<>();
        final Map<String, Integer> cost = Map.of("bookmark", 30, "tote", 80);
        final Map<String, Integer> stock = new ConcurrentHashMap<>(Map.of("bookmark", 5, "tote", 2));

        Service() {
            for (int i = 1; i <= 8; i++) balances.put("u" + i, 100);
        }

        Map<String, Object> award(Map<String, Object> in) {
            String u = (String) in.get("userId");
            int points = (Integer) in.get("points");
            if (points <= 0 || !balances.containsKey(u)) return errors("Unknown user or non-positive points.", "INVALID");
            balances.put(u, balances.get(u) + points);
            return Map.of("balance", balances.get(u), "userErrors", List.of());
        }

        Map<String, Object> transfer(Map<String, Object> in) {
            String from = (String) in.get("fromUserId"), to = (String) in.get("toUserId");
            int amount = (Integer) in.get("amount");
            if (amount <= 0 || from.equals(to) || !balances.containsKey(from) || !balances.containsKey(to)) return errors("Invalid transfer.", "INVALID");
            if (balances.get(from) < amount) return errors("Not enough points.", "INSUFFICIENT_POINTS");
            balances.put(from, balances.get(from) - amount);
            balances.put(to, balances.get(to) + amount);
            return Map.of("balance", balances.get(from), "userErrors", List.of());
        }

        Map<String, Object> redeem(Map<String, Object> in) {
            String u = (String) in.get("userId"), r = (String) in.get("rewardId");
            if (!cost.containsKey(r) || !balances.containsKey(u)) return errors("Unknown user or reward.", "INVALID");
            if (stock.get(r) == 0) return errors("Out of stock.", "OUT_OF_STOCK");
            if (balances.get(u) < cost.get(r)) return errors("Not enough points.", "INSUFFICIENT_POINTS");
            balances.put(u, balances.get(u) - cost.get(r));
            stock.put(r, stock.get(r) - 1);
            return Map.of("balance", balances.get(u), "remainingStock", stock.get(r), "userErrors", List.of());
        }
    }

    static Map<String, Object> errors(String message, String code) {
        return Map.of("userErrors", List.of(Map.of("message", message, "code", code)));
    }

    public static GraphQL graphQL() {
        Service s = new Service();
        try {
            var wiring = RuntimeWiring.newRuntimeWiring()
                    .type("Query", t -> t
                            .dataFetcher("balance", env -> s.balances.getOrDefault(env.<String>getArgument("userId"), 0))
                            .dataFetcher("stock", env -> s.stock.getOrDefault(env.<String>getArgument("rewardId"), 0)))
                    .type("Mutation", t -> t
                            .dataFetcher("awardPoints", env -> s.award(env.getArgument("input")))
                            .dataFetcher("transferPoints", env -> s.transfer(env.getArgument("input")))
                            .dataFetcher("redeemReward", env -> s.redeem(env.getArgument("input"))))
                    .build();
            return GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(
                    new SchemaParser().parse(Files.readString(Path.of("points.graphqls"))), wiring)).build();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
