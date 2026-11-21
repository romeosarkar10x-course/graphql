package dev.shelfie.secure;

import graphql.ExecutionResult;
import graphql.GraphqlErrorBuilder;
import graphql.analysis.QueryComplexityCalculator;
import graphql.execution.AbortExecutionException;
import graphql.execution.instrumentation.*;
import graphql.execution.instrumentation.parameters.*;
import graphql.schema.DataFetcher;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lecture 10.6: a public API cannot allowlist its clients' documents, so it identifies each client app
 * (X-Api-Key), limits what it may do (scopes) and how much (a cost budget, a leaky bucket in the style of
 * Shopify's), and says so on every response:
 *
 *   "extensions": {"cost": {"requestedQueryCost": 140,
 *                           "throttleStatus": {"maximumAvailable": 1000, "currentlyAvailable": 860, "restoreRate": 50}}}
 *
 * The end user is still the bearer token; the key is the app acting for them.
 */
@Configuration
public class PublicApi {
  record Client(String name, Set<String> scopes, int maximum, int restorePerSecond) {}

  // In production: a table, hashed keys, a portal to rotate them. Here: four fixed clients.
  static final Map<String, Client> KEYS = Map.of(
      "pk_reader_1", new Client("bookworm-app", Set.of("read"), 1000, 50),
      "pk_writer_2", new Client("review-bot", Set.of("read", "write:reviews"), 1000, 50),
      "pk_spender_3", new Client("spender", Set.of("read"), 1000, 1000),
      "pk_admin_4", new Client("club-tool", Set.of("read", "write:reviews", "write:clubs"), 1000, 50));
  static final Map<String, String> SCOPE_FOR = Map.of(
      "addReview", "write:reviews", "removeReview", "write:reviews", "renameClub", "write:clubs", "joinClub", "write:clubs");

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
  private final Map<String, Map<String, AtomicLong>> usage = new ConcurrentHashMap<>();

  /** No key or an unknown key: 401 before GraphQL runs, like a bad token. */
  @Bean
  OncePerRequestFilter apiKeyFilter() {
    return new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, jakarta.servlet.ServletException {
        if (req.getRequestURI().equals("/graphql") && !KEYS.containsKey(String.valueOf(req.getHeader("X-Api-Key")))) {
          res.setStatus(401);
          res.setContentType("application/json");
          res.getWriter().write("{\"errors\":[{\"message\":\"a valid X-Api-Key header is required\",\"extensions\":{\"code\":\"INVALID_API_KEY\"}}]}");
          return;
        }
        chain.doFilter(req, res);
      }
    };
  }

  @Bean
  WebGraphQlInterceptor apiClient() {
    return (request, chain) -> {
      Client c = KEYS.get(request.getHeaders().getFirst("X-Api-Key"));
      request.configureExecutionInput((input, b) -> b.graphQLContext(ctx -> ctx.put("client", c)).build());
      return chain.next(request);
    };
  }

  private void count(Client c, String what) {
    usage.computeIfAbsent(c.name(), k -> new ConcurrentHashMap<>()).computeIfAbsent(what, k -> new AtomicLong()).incrementAndGet();
  }

  @Bean
  Instrumentation scopesAndBudget() {
    return new SimplePerformantInstrumentation() {
      @Override
      public DataFetcher<?> instrumentDataFetcher(DataFetcher<?> fetcher, InstrumentationFieldFetchParameters p, InstrumentationState s) {
        String needed = p.getEnvironment().getParentType() instanceof graphql.schema.GraphQLObjectType t && t.getName().equals("Mutation")
            ? SCOPE_FOR.get(p.getEnvironment().getField().getName()) : null;
        if (needed == null) return fetcher;
        return env -> {
          Client c = env.getGraphQlContext().get("client");
          if (!c.scopes().contains(needed))
            throw new Denied(ErrorType.FORBIDDEN, c.name() + " lacks the " + needed + " scope");
          return fetcher.get(env);
        };
      }

      @Override
      public InstrumentationContext<ExecutionResult> beginExecuteOperation(InstrumentationExecuteOperationParameters p, InstrumentationState s) {
        var ctx = p.getExecutionContext();
        Client c = ctx.getGraphQLContext().get("client");
        int cost = QueryComplexityCalculator.newCalculator().fieldComplexityCalculator(DemandControl.COST).schema(ctx.getGraphQLSchema())
            .document(ctx.getDocument()).operationName(ctx.getOperationDefinition().getName())
            .variables(ctx.getCoercedVariables()).build().calculate();
        if (cost > DemandControl.MAX_COST) return SimpleInstrumentationContext.noOp();
        Bucket bucket = buckets.computeIfAbsent(c.name(), k -> Bucket.builder()
            .addLimit(l -> l.capacity(c.maximum()).refillGreedy(c.restorePerSecond(), Duration.ofSeconds(1))).build());
        var probe = bucket.tryConsumeAndReturnRemaining(Math.max(cost, 1));   // { __typename } costs 0; Bucket4j refuses 0
        Map<String, Object> status = Map.of("requestedQueryCost", cost, "throttleStatus", Map.of(
            "maximumAvailable", c.maximum(), "currentlyAvailable", probe.getRemainingTokens(), "restoreRate", c.restorePerSecond()));
        if (!probe.isConsumed()) {
          count(c, "throttled");
          throw new AbortExecutionException(List.of(GraphqlErrorBuilder.newError().errorType(ErrorType.BAD_REQUEST)
              .message("Throttled: this query costs " + cost + " and " + probe.getRemainingTokens() + " points are available")
              .extensions(Map.of("code", "THROTTLED", "cost", status)).build()));
        }
        count(c, "served");
        ctx.getGraphQLContext().put("cost", status);
        return SimpleInstrumentationContext.noOp();
      }

      @Override
      public CompletableFuture<ExecutionResult> instrumentExecutionResult(ExecutionResult r, InstrumentationExecutionParameters p, InstrumentationState s) {
        Object cost = p.getGraphQLContext().get("cost");
        return CompletableFuture.completedFuture(cost == null ? r : r.transform(b -> b.addExtension("cost", cost)));
      }
    };
  }

  /** Abuse monitoring, the simplest version: requests served and throttled per client. */
  @RestController
  static class Usage {
    private final PublicApi api;

    Usage(PublicApi api) { this.api = api; }

    @GetMapping("/_admin/usage")
    Map<String, Map<String, AtomicLong>> usage() { return api.usage; }
  }
}
