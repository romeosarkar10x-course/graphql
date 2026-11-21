import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentationContext;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 5.5: find Shelfie's slow fields with an Instrumentation.
 *   report <query.graphql>   "slow <Type.field> <ms> ms" per coordinate slower than 50 ms, slowest first,
 *                            then "slowest <Type.field>" and "fetches <n>"
 *   run <query.graphql>      the spec response map, with extensions.timing = {fetches, slowFields}
 *
 * TODO: this instrumentation times the whole request, which says nothing about which field is slow.
 * Time each field's fetch, keep the numbers per request (not in a static), report them through
 * extensions, and run it chained with Latency, which is what makes some fields slow.
 */
public class Timing {
    static final long THRESHOLD_MS = 50;

    static final class FieldTiming extends SimplePerformantInstrumentation {
        @Override
        public InstrumentationContext<ExecutionResult> beginExecution(InstrumentationExecutionParameters p, InstrumentationState state) {
            long start = System.nanoTime();
            return SimpleInstrumentationContext.whenCompleted((r, t) ->
                System.err.println("request took " + (System.nanoTime() - start) / 1_000_000 + " ms"));
        }
    }

    public static void main(String[] args) throws Exception {
        var graphQL = GraphQL.newGraphQL(Shelfie.schema(Path.of("shelfie.graphqls")))
            .instrumentation(new FieldTiming())
            .build();
        var result = graphQL.execute(ExecutionInput.newExecutionInput(Files.readString(Path.of(args[1]))).build());
        if (args[0].equals("run")) {
            System.out.println(JsonMapper.builder().build().writeValueAsString(result.toSpecification()));
            return;
        }
        @SuppressWarnings("unchecked")
        var t = result.getExtensions();
        System.out.println("slowest " + (t == null ? "none" : t));
    }
}
