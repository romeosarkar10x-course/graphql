import graphql.GraphQL;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Game 4a: points that survive the chaos button. Every call to graphQL() is a fresh service:
 * users u1..u8 with 100 points; rewards "bookmark" (30 points, 5 in stock) and "tote" (80 points, 2).
 */
public final class Points {
    private Points() {}

    record Reward(int cost, int stock) {}

    /** One service instance. All state behind one lock: correctness first; Module 9 measures throughput. */
    static final class Service {
        final Map<String, Integer> balances = new HashMap<>();
        final Map<String, Reward> rewards = new HashMap<>();
        /** requestId -> the payload we answered with. A retry gets the same answer, and no second effect. */
        final Map<String, Map<String, Object>> answered = new HashMap<>();

        Service() {
            for (int i = 1; i <= 8; i++) balances.put("u" + i, 100);
            rewards.put("bookmark", new Reward(30, 5));
            rewards.put("tote", new Reward(80, 2));
        }

        synchronized Map<String, Object> once(Map<String, Object> input, java.util.function.Supplier<Map<String, Object>> effect) {
            return answered.computeIfAbsent((String) input.get("requestId"), k -> effect.get());
        }

        Map<String, Object> award(Map<String, Object> in) {
            return once(in, () -> {
                String u = (String) in.get("userId");
                int points = (Integer) in.get("points");
                if (points <= 0 || !balances.containsKey(u)) return errors("Unknown user or non-positive points.", "INVALID");
                balances.merge(u, points, Integer::sum);
                return Map.of("balance", balances.get(u), "userErrors", List.of());
            });
        }

        Map<String, Object> transfer(Map<String, Object> in) {
            return once(in, () -> {
                String from = (String) in.get("fromUserId"), to = (String) in.get("toUserId");
                int amount = (Integer) in.get("amount");
                if (amount <= 0 || from.equals(to) || !balances.containsKey(from) || !balances.containsKey(to)) {
                    return errors("Invalid transfer.", "INVALID");
                }
                if (balances.get(from) < amount) return errors("Not enough points.", "INSUFFICIENT_POINTS");
                balances.merge(from, -amount, Integer::sum);
                balances.merge(to, amount, Integer::sum);
                return Map.of("balance", balances.get(from), "userErrors", List.of());
            });
        }

        Map<String, Object> redeem(Map<String, Object> in) {
            return once(in, () -> {
                String u = (String) in.get("userId");
                Reward r = rewards.get((String) in.get("rewardId"));
                if (r == null || !balances.containsKey(u)) return errors("Unknown user or reward.", "INVALID");
                if (r.stock() == 0) return errors("Out of stock.", "OUT_OF_STOCK");
                if (balances.get(u) < r.cost()) return errors("Not enough points.", "INSUFFICIENT_POINTS");
                balances.merge(u, -r.cost(), Integer::sum);
                rewards.put((String) in.get("rewardId"), new Reward(r.cost(), r.stock() - 1));
                return Map.of("balance", balances.get(u), "remainingStock", r.stock() - 1, "userErrors", List.of());
            });
        }

        synchronized int balance(String u) { return balances.getOrDefault(u, 0); }
        synchronized int stock(String r) { return rewards.containsKey(r) ? rewards.get(r).stock() : 0; }
    }

    static Map<String, Object> errors(String message, String code) {
        return Map.of("userErrors", List.of(Map.of("message", message, "code", code)));
    }

    public static GraphQL graphQL() {
        Service s = new Service();
        try {
            var wiring = RuntimeWiring.newRuntimeWiring()
                    .type("Query", t -> t
                            .dataFetcher("balance", env -> s.balance(env.getArgument("userId")))
                            .dataFetcher("stock", env -> s.stock(env.getArgument("rewardId"))))
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
