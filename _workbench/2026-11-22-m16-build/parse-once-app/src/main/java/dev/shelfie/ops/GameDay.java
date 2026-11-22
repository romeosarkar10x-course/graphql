package dev.shelfie.ops;

import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLObjectType;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebSocketGraphQlRequest;

/**
 * Incident game day (lab 16.8). Only with --gameday.seed=N; the labs' copies of this app do not have this class.
 * Spoiler for the game: do not read on if you are playing. The seed picks three faults:
 *
 *   1. one field gets slow (about 250 ms more per call);
 *   2. one client's subscriptions are never released when they end: each keeps 2 MB on the heap, and the
 *      shelfie_subscriptions_open gauge for that client only goes up;
 *   3. one client (the traffic generator plays it) sends expensive queries far too often.
 */
@Configuration
@ConditionalOnProperty("gameday.seed")
public class GameDay {
    static final List<String> SLOW = List.of("Club.members", "Club.currentBook", "Query.search", "Query.book");
    static final List<String> CLIENTS = List.of("web", "ios", "android", "partner-bulk");

    /** The answers for a seed: {slowField, leakingClient, abusiveClient}. GameTraffic uses the same function. */
    public static Map<String, String> answers(long seed) {
        Random r = new Random(seed * 7919 + 16);
        String slow = SLOW.get(r.nextInt(SLOW.size()));
        String leak = CLIENTS.get(r.nextInt(CLIENTS.size()));
        String abusive;
        do { abusive = CLIENTS.get(r.nextInt(CLIENTS.size())); } while (abusive.equals(leak));
        return Map.of("slowField", slow, "leakingClient", leak, "abusiveClient", abusive);
    }

    private final Map<String, String> faults;
    private final Map<String, AtomicInteger> open = new ConcurrentHashMap<>();
    private final List<byte[]> retained = new ArrayList<>();

    public GameDay(@org.springframework.beans.factory.annotation.Value("${gameday.seed}") long seed, MeterRegistry meters) {
        this.faults = answers(seed);
        for (String c : CLIENTS) {
            AtomicInteger n = new AtomicInteger();
            open.put(c, n);
            Gauge.builder("shelfie.subscriptions.open", n, AtomicInteger::get).tag("graphql.client.name", c)
                    .description("Subscriptions open right now, by client").register(meters);
        }
    }

    @Bean
    SimplePerformantInstrumentation slowField() {
        String slow = faults.get("slowField");
        return new SimplePerformantInstrumentation() {
            @Override
            public DataFetcher<?> instrumentDataFetcher(DataFetcher<?> fetcher, InstrumentationFieldFetchParameters p,
                                                        InstrumentationState state) {
                var env = p.getEnvironment();
                String type = env.getParentType() instanceof GraphQLObjectType o ? o.getName() : "";
                if (!slow.equals(type + "." + env.getField().getName())) return fetcher;
                return e -> {
                    Thread.sleep(220 + (long) (Math.random() * 60));
                    return fetcher.get(e);
                };
            }
        };
    }

    @Bean
    WebGraphQlInterceptor subscriptionLeak() {
        String leaking = faults.get("leakingClient");
        return (request, chain) -> {
            if (!(request instanceof WebSocketGraphQlRequest) || !request.getDocument().contains("subscription"))
                return chain.next(request);
            String client = request.getHeaders().getFirst("graphql-client-name");
            AtomicInteger n = client == null ? null : open.get(client);
            if (n == null) return chain.next(request);
            n.incrementAndGet();
            if (client.equals(leaking)) synchronized (retained) { retained.add(new byte[2 * 1024 * 1024]); }
            boolean leaks = client.equals(leaking);
            return chain.next(request).map(response -> {
                if (!(response.getExecutionResult().getData() instanceof Publisher<?> events)) {
                    if (!leaks) n.decrementAndGet();
                    return response;
                }
                Flux<?> tracked = Flux.from(events).doFinally(signal -> { if (!leaks) n.decrementAndGet(); });
                return response.transform(b -> b.data(tracked));
            });
        };
    }
}
