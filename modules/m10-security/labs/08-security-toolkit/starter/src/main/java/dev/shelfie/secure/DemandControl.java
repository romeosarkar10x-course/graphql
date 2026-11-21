package dev.shelfie.secure;

import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.analysis.FieldComplexityCalculator;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.QueryComplexityCalculator;
import graphql.execution.AbortExecutionException;
import graphql.execution.instrumentation.*;
import graphql.execution.instrumentation.parameters.*;
import graphql.schema.DataFetcher;
import graphql.validation.QueryComplexityLimits;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.server.WebGraphQlInterceptor;

/**
 * Lecture 10.4: five limits, each stopping a different attack from lab 10.3.
 *
 *   1. depth 10 and 500 fields, graphql-java 26's QueryComplexityLimits, per request (deep nesting, aliases)
 *   2. cost 5,000, MaxQueryComplexityInstrumentation with list sizes multiplied in (fan-out)
 *   3. first <= 50 on every list (one huge page)
 *   4. 1 second per request, by cancelling the ExecutionInput (slow fields)
 *   5. 10,000 cost points a minute per caller, a Bucket4j token bucket (brute force, scraping)
 *
 * Shelfie's real screens (tests/real.hurl) cost 30 to 450 and stay well inside all five.
 */
@Configuration
public class DemandControl {
  static final int MAX_DEPTH = 10, MAX_FIELDS = 500, MAX_COST = 5_000, MAX_FIRST = 50, BUDGET_PER_MINUTE = 10_000;
  static final Duration TIMEOUT = Duration.ofSeconds(1);

  /** A field costs 1 (10 for the slow recommender) plus its children, times the page size for lists. */
  static final FieldComplexityCalculator COST = (env, childCost) -> {
    int own = env.getField().getName().equals("recommendations") ? 10 : 1;
    Object first = env.getArguments().get("first");
    return first instanceof Integer n ? Math.max(n, 1) * (own + childCost) : own + childCost;
  };

  /** 1: per-request structural limits (graphql-java's defaults are depth 100 and 100,000 fields). */
  @Bean
  WebGraphQlInterceptor structuralLimits() {
    QueryComplexityLimits limits = QueryComplexityLimits.newLimits().maxDepth(MAX_DEPTH).maxFieldsCount(MAX_FIELDS).build();
    return (request, chain) -> {
      String caller = request.getRemoteAddress() == null ? "unknown" : request.getRemoteAddress().getHostString();
      request.configureExecutionInput((input, b) -> b.graphQLContext(c -> c.put(QueryComplexityLimits.KEY, limits).put("ip", caller)).build());
      return chain.next(request);
    };
  }

  /** 2: the cost limit. */
  @Bean
  Instrumentation costLimit() { return new MaxQueryComplexityInstrumentation(MAX_COST, COST); }

  /** 3 and 4: list caps on every argument named first, and the timeout. */
  @Bean
  Instrumentation capsAndTimeout() {
    ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    return new SimplePerformantInstrumentation() {
      @Override
      public DataFetcher<?> instrumentDataFetcher(DataFetcher<?> fetcher, InstrumentationFieldFetchParameters p, InstrumentationState s) {
        return env -> {
          if (env.getArgument("first") instanceof Integer n && (n < 0 || n > MAX_FIRST))
            throw new Denied(ErrorType.BAD_REQUEST, "first must be between 0 and " + MAX_FIRST + ", was " + n);
          return fetcher.get(env);
        };
      }

      @Override
      public InstrumentationContext<ExecutionResult> beginExecution(InstrumentationExecutionParameters p, InstrumentationState s) {
        ScheduledFuture<?> cancel = timer.schedule(p.getExecutionInput()::cancel, TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        return SimpleInstrumentationContext.whenCompleted((result, error) -> cancel.cancel(false));
      }
    };
  }

  /** 5: a budget of cost points per caller per minute, reported in extensions.cost. */
  @Bean
  Instrumentation costBudget(Viewer viewer) {
    Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    return new SimplePerformantInstrumentation() {
      @Override
      public InstrumentationContext<ExecutionResult> beginExecuteOperation(InstrumentationExecuteOperationParameters p, InstrumentationState s) {
        var ctx = p.getExecutionContext();
        int cost = QueryComplexityCalculator.newCalculator().fieldComplexityCalculator(COST).schema(ctx.getGraphQLSchema())
            .document(ctx.getDocument()).operationName(ctx.getOperationDefinition().getName())
            .variables(ctx.getCoercedVariables()).build().calculate();
        if (cost > MAX_COST) return SimpleInstrumentationContext.noOp();   // the cost limit refuses it anyway
        Reader me = viewer.current();
        String caller = me != null ? me.id() : "anonymous@" + ctx.getGraphQLContext().get("ip");
        Bucket bucket = buckets.computeIfAbsent(caller, k -> Bucket.builder()
            .addLimit(l -> l.capacity(BUDGET_PER_MINUTE).refillGreedy(BUDGET_PER_MINUTE, Duration.ofMinutes(1))).build());
        var probe = bucket.tryConsumeAndReturnRemaining(Math.max(cost, 1));   // { __typename } costs 0; Bucket4j refuses 0
        if (!probe.isConsumed()) {
          long retry = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1);
          GraphQLError error = GraphqlErrorBuilder.newError().errorType(ErrorType.BAD_REQUEST)
              .message("rate limited: this query costs " + cost + ", " + probe.getRemainingTokens() + " of "
                  + BUDGET_PER_MINUTE + " points left this minute")
              .extensions(Map.of("code", "RATE_LIMITED", "cost", cost, "retryAfterSeconds", retry)).build();
          throw new AbortExecutionException(List.of(error));
        }
        ctx.getGraphQLContext().put("cost", Map.of("requested", cost, "remaining", probe.getRemainingTokens(), "limit", BUDGET_PER_MINUTE));
        return SimpleInstrumentationContext.noOp();
      }

      @Override
      public CompletableFuture<ExecutionResult> instrumentExecutionResult(ExecutionResult r, InstrumentationExecutionParameters p, InstrumentationState s) {
        Object cost = p.getGraphQLContext().get("cost");
        return CompletableFuture.completedFuture(cost == null ? r : r.transform(b -> b.addExtension("cost", cost)));
      }
    };
  }
}
