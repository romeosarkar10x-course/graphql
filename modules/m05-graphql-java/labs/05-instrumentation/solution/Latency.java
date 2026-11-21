import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import java.util.Map;

/**
 * Provided: pretends some of Shelfie's fields call slow backends, by sleeping before their fetcher.
 * Do not edit; the test expects these delays.
 */
final class Latency extends SimplePerformantInstrumentation {
    static final Map<String, Integer> DELAY_MS = Map.of(
        "Book.averageRating", 120,   // a ratings service
        "Club.meetups", 75,          // a calendar API
        "Query.clubs", 60,           // a slow SQL query
        "User.reviews", 15,
        "Review.author", 2);

    @Override
    public DataFetcher<?> instrumentDataFetcher(DataFetcher<?> df, InstrumentationFieldFetchParameters p, InstrumentationState s) {
        var info = p.getExecutionStepInfo();
        Integer ms = DELAY_MS.get(info.getObjectType().getName() + "." + info.getFieldDefinition().getName());
        if (ms == null) return df;
        return env -> { Thread.sleep(ms); return df.get(env); };
    }
}
