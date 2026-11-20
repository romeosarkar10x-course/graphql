import graphql.ExecutionInput;
import graphql.GraphQL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * The chaos button. For one seed: 2,000 random mutations from 16 concurrent clients, through a network
 * that drops responses (the client retries with the same requestId), duplicates requests (both copies
 * arrive), and retries late. Then it checks what must always hold.
 *
 *   java Chaos <seed> [--calm]      --calm: no chaos, one client (what a hand-written test sees)
 */
public class Chaos {
    static final String[] USERS = {"u1", "u2", "u3", "u4", "u5", "u6", "u7", "u8"};
    static final Map<String, Integer> COST = Map.of("bookmark", 30, "tote", 80);
    static final Map<String, Integer> STOCK = Map.of("bookmark", 5, "tote", 2);

    record Op(String requestId, String mutation, Map<String, Object> input) {}

    static final String AWARD = "mutation($i: AwardPointsInput!) { awardPoints(input: $i) { balance userErrors { code } } }";
    static final String TRANSFER = "mutation($i: TransferPointsInput!) { transferPoints(input: $i) { balance userErrors { code } } }";
    static final String REDEEM = "mutation($i: RedeemRewardInput!) { redeemReward(input: $i) { balance remainingStock userErrors { code } } }";

    @SuppressWarnings("unchecked")
    static Map<String, Object> send(GraphQL g, Op op) {
        var r = g.execute(ExecutionInput.newExecutionInput(op.mutation()).variables(Map.of("i", op.input())).build());
        if (!r.getErrors().isEmpty()) throw new IllegalStateException(r.getErrors().getFirst().getMessage());
        return (Map<String, Object>) ((Map<String, Object>) r.getData()).values().iterator().next();
    }

    public static void main(String[] args) throws Exception {
        long seed = Long.parseLong(args[0]);
        boolean calm = args.length > 1 && args[1].equals("--calm");
        Random rnd = new Random(seed);
        List<Op> ops = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            String id = "s" + seed + "-" + i;
            double p = rnd.nextDouble();
            if (p < 0.4) ops.add(new Op(id, AWARD, Map.of("requestId", id, "userId", USERS[rnd.nextInt(8)], "points", 1 + rnd.nextInt(20))));
            else if (p < 0.8) {
                String from = USERS[rnd.nextInt(8)], to = USERS[rnd.nextInt(8)];
                if (from.equals(to)) to = USERS[(rnd.nextInt(7) + 1 + List.of(USERS).indexOf(from)) % 8];
                ops.add(new Op(id, TRANSFER, Map.of("requestId", id, "fromUserId", from, "toUserId", to, "amount", 1 + rnd.nextInt(60))));
            } else ops.add(new Op(id, REDEEM, Map.of("requestId", id, "userId", USERS[rnd.nextInt(8)], "rewardId", rnd.nextBoolean() ? "bookmark" : "tote")));
        }
        GraphQL g = Points.graphQL();
        Map<String, List<Map<String, Object>>> seen = new ConcurrentHashMap<>();   // every response the client received, per requestId
        List<String> crashes = Collections.synchronizedList(new ArrayList<>());
        int[] drops = {0}, dups = {0};
        long[] chaosSeeds = new long[ops.size()];
        for (int i = 0; i < chaosSeeds.length; i++) chaosSeeds[i] = rnd.nextLong();
        try (var pool = Executors.newFixedThreadPool(calm ? 1 : 16, Thread.ofVirtual().factory())) {
            for (int i = 0; i < ops.size(); i++) {
                Op op = ops.get(i);
                Random r = new Random(chaosSeeds[i]);
                pool.submit(() -> {
                    try {
                        double c = calm ? 1.0 : r.nextDouble();
                        if (c < 0.2) {                       // response lost: the client retries
                            send(g, op);
                            synchronized (drops) { drops[0]++; }
                            seen.computeIfAbsent(op.requestId(), k -> Collections.synchronizedList(new ArrayList<>())).add(send(g, op));
                        } else if (c < 0.3) {                // request duplicated in flight: both arrive at once
                            synchronized (dups) { dups[0]++; }
                            var twin = Thread.ofVirtual().start(() -> seen.computeIfAbsent(op.requestId(), k -> Collections.synchronizedList(new ArrayList<>())).add(send(g, op)));
                            seen.computeIfAbsent(op.requestId(), k -> Collections.synchronizedList(new ArrayList<>())).add(send(g, op));
                            twin.join();
                        } else if (c < 0.4) {                // a slow retry arrives after the answer
                            var first = send(g, op);
                            seen.computeIfAbsent(op.requestId(), k -> Collections.synchronizedList(new ArrayList<>())).add(first);
                            seen.get(op.requestId()).add(send(g, op));
                        } else {
                            seen.computeIfAbsent(op.requestId(), k -> Collections.synchronizedList(new ArrayList<>())).add(send(g, op));
                        }
                    } catch (Throwable t) {
                        crashes.add(op.requestId() + ": " + t.getMessage());
                    }
                });
            }
        }
        // ---- what must hold
        List<String> broken = new ArrayList<>();
        if (!crashes.isEmpty()) broken.add(crashes.size() + " request(s) threw, first: " + crashes.getFirst());
        int awarded = 0, inconsistent = 0;
        Map<String, Integer> redeemed = new HashMap<>(Map.of("bookmark", 0, "tote", 0));
        for (Op op : ops) {
            List<Map<String, Object>> rs = seen.getOrDefault(op.requestId(), List.of());
            if (rs.isEmpty()) continue;
            if (rs.stream().distinct().count() > 1) inconsistent++;
            Map<String, Object> answer = rs.getFirst();
            if (!((List<?>) answer.get("userErrors")).isEmpty()) continue;
            if (op.mutation() == AWARD) awarded += (Integer) op.input().get("points");
            if (op.mutation() == REDEEM) redeemed.merge((String) op.input().get("rewardId"), 1, Integer::sum);
        }
        if (inconsistent > 0) broken.add(inconsistent + " requestId(s) got different answers on retry (a retry must replay the first answer)");
        int total = 0, negative = 0;
        for (String u : USERS) {
            int b = (Integer) ((Map<String, Object>) g.execute("{ balance(userId: \"" + u + "\") }").getData()).get("balance");
            total += b;
            if (b < 0) negative++;
        }
        int spent = redeemed.get("bookmark") * COST.get("bookmark") + redeemed.get("tote") * COST.get("tote");
        int expected = 800 + awarded - spent;
        if (total != expected) broken.add("points not conserved: balances sum to " + total + ", the answers clients saw add up to " + expected);
        if (negative > 0) broken.add(negative + " user(s) with a negative balance");
        for (String reward : STOCK.keySet()) {
            int left = (Integer) ((Map<String, Object>) g.execute("{ stock(rewardId: \"" + reward + "\") }").getData()).get("stock");
            if (left < 0) broken.add(reward + " stock is negative: " + left);
            if (STOCK.get(reward) - left != redeemed.get(reward)) broken.add(reward + ": " + (STOCK.get(reward) - left) + " left the shelf, clients were told " + redeemed.get(reward));
        }
        String summary = "seed " + seed + (calm ? " (calm)" : "") + ": 2000 ops, " + drops[0] + " lost responses, " + dups[0] + " duplicated requests";
        if (broken.isEmpty()) System.out.println("pass " + summary);
        else { System.out.println("fail " + summary); broken.forEach(b -> System.out.println("     " + b)); }
        System.exit(broken.isEmpty() ? 0 : 1);
    }
}
