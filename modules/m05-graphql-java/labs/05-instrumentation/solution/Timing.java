import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentationContext;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationCreateStateParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 5.5: find Shelfie's slow fields with an Instrumentation.
 *   report <query.graphql>   "slow <Type.field> <ms> ms" per coordinate slower than 50 ms, slowest first,
 *                            then "slowest <Type.field>" and "fetches <n>"
 *   run <query.graphql>      the spec response map, with extensions.timing added by the instrumentation
 */
public class Timing {
    static final long THRESHOLD_MS = 50;

    /** Per-request state: the slowest fetch seen for each coordinate, and how many fetches ran. */
    static final class Times implements InstrumentationState {
        final Map<String, Long> maxMs = new ConcurrentHashMap<>();
        final AtomicInteger fetches = new AtomicInteger();

        List<Map.Entry<String, Long>> slow() {
            return maxMs.entrySet().stream().filter(e -> e.getValue() > THRESHOLD_MS)
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()).toList();
        }
    }

    static final class FieldTiming extends SimplePerformantInstrumentation {
        @Override
        public CompletableFuture<InstrumentationState> createStateAsync(InstrumentationCreateStateParameters p) {
            return CompletableFuture.completedFuture(new Times());
        }

        /** Fetch only: from calling the fetcher until its value (or future) completes; children not included. */
        @Override
        public InstrumentationContext<Object> beginFieldFetch(InstrumentationFieldFetchParameters p, InstrumentationState state) {
            var times = (Times) state;
            var info = p.getExecutionStepInfo();
            String coordinate = info.getObjectType().getName() + "." + info.getFieldDefinition().getName();
            long start = System.nanoTime();
            return SimpleInstrumentationContext.whenCompleted((value, error) -> {
                times.fetches.incrementAndGet();
                times.maxMs.merge(coordinate, (System.nanoTime() - start) / 1_000_000, Math::max);
            });
        }

        /** Report through the response itself: extensions is the spec's place for it. */
        @Override
        public CompletableFuture<ExecutionResult> instrumentExecutionResult(ExecutionResult result, InstrumentationExecutionParameters p, InstrumentationState state) {
            var times = (Times) state;
            var timing = new LinkedHashMap<String, Object>();
            timing.put("fetches", times.fetches.get());
            timing.put("slowFields", times.slow().stream().map(Map.Entry::getKey).toList());
            timing.put("slowMs", times.slow().stream().map(Map.Entry::getValue).toList());
            return CompletableFuture.completedFuture(result.transform(b -> b.addExtension("timing", timing)));
        }
    }

    public static void main(String[] args) throws Exception {
        var timing = new FieldTiming();
        var graphQL = GraphQL.newGraphQL(Shelfie.schema(Path.of("shelfie.graphqls")))
            .instrumentation(new ChainedInstrumentation(List.of(new Latency(), timing)))
            .build();
        var result = graphQL.execute(ExecutionInput.newExecutionInput(Files.readString(Path.of(args[1]))).build());
        if (args[0].equals("run")) {
            System.out.println(JsonMapper.builder().build().writeValueAsString(result.toSpecification()));
            return;
        }
        @SuppressWarnings("unchecked")
        var t = (Map<String, Object>) result.getExtensions().get("timing");
        @SuppressWarnings("unchecked")
        var slow = (List<String>) t.get("slowFields");
        @SuppressWarnings("unchecked")
        var ms = (List<Long>) t.get("slowMs");
        for (int i = 0; i < slow.size(); i++) System.out.println("slow " + slow.get(i) + " " + ms.get(i) + " ms");
        System.out.println("slowest " + (slow.isEmpty() ? "none" : slow.get(0)));
        System.out.println("fetches " + t.get("fetches"));
    }
}
