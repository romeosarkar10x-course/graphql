package dev.shelfie.ops;

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.analysis.QueryTraverser;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLType;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;

/**
 * Module 16.6: what a request costs, before it runs, as a metric an autoscaler can use.
 *
 * Cost (the course's rule, also in the lab README): every field selected costs 1, times the number of items its
 * parents return. A list field returns `first` items when it has that argument, otherwise its expected size below.
 * So  { books(first: 2) { title similar { title } } }  costs  1 (books) + 2 (title) + 2 (similar) + 2*3 (title) = 11.
 *
 *   graphql_cost_sum / graphql_cost_count{graphql_client_name, graphql_operation_name}   (DistributionSummary)
 *   "extensions": {"cost": 11}                                                         on every response
 */
@Component
public class CostMetric extends SimplePerformantInstrumentation {
    static final Map<String, Integer> EXPECTED = Map.of(
            "Query.books", 10, "Query.clubs", 3, "Query.search", 3,
            "Club.members", 6, "Club.readingList", 4, "Book.reviews", 5, "Book.similar", 3);

    private final MeterRegistry meters;

    public CostMetric(MeterRegistry meters) { this.meters = meters; }

    /** The cost of the operation the request selects (0 when the document does not validate). */
    static int cost(InstrumentationExecutionParameters p) {
        try {
            var traverser = QueryTraverser.newQueryTraverser().schema(p.getSchema())
                    .document(Documents.parse(p.getQuery()))
                    .operationName(p.getOperation()).variables(p.getVariables()).build();
            return traverser.reducePreOrder((env, sum) -> sum + multiplier(env.getParentEnvironment()), 0);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /** How many times a field's selection runs: the product of its ancestors' list sizes. */
    static int multiplier(QueryVisitorFieldEnvironment env) {
        if (env == null) return 1;
        return multiplier(env.getParentEnvironment()) * size(env);
    }

    static int size(QueryVisitorFieldEnvironment env) {
        GraphQLType type = env.getFieldDefinition().getType();
        if (type instanceof GraphQLNonNull nn) type = nn.getWrappedType();
        if (!(type instanceof GraphQLList)) return 1;
        if (env.getArguments().get("first") instanceof Integer first) return first;
        return EXPECTED.getOrDefault(env.getFieldsContainer().getName() + "." + env.getFieldDefinition().getName(), 1);
    }

    @Override
    public CompletableFuture<ExecutionResult> instrumentExecutionResult(ExecutionResult result,
            InstrumentationExecutionParameters p, InstrumentationState state) {
        int cost = cost(p);
        String client = p.getGraphQLContext().getOrDefault(Observability.CLIENT, "unknown");
        String operation = p.getOperation() == null ? "anonymous" : p.getOperation();
        DistributionSummary.builder("graphql.cost").description("Static cost of each operation")
                .tag("graphql.client.name", client).tag("graphql.operation.name", operation)
                .register(meters).record(cost);
        Map<Object, Object> extensions = new LinkedHashMap<>();
        if (result.getExtensions() != null) extensions.putAll(result.getExtensions());
        extensions.put("cost", cost);
        return CompletableFuture.completedFuture(ExecutionResultImpl.newExecutionResult().from(result)
                .extensions(extensions).build());
    }
}
