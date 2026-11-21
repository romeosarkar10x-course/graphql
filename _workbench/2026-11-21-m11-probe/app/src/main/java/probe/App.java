package probe;

import graphql.ExperimentalApi;
import graphql.GraphQL;
import graphql.ExecutionInput;
import graphql.incremental.IncrementalExecutionResult;
import java.time.Duration;
import java.util.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.graphql.autoconfigure.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@SpringBootApplication
@Controller
public class App {
  public static void main(String[] a) { SpringApplication.run(App.class, a); }

  record Club(String id, String name) {}

  @QueryMapping String hello() { return "hi"; }
  @QueryMapping Club club(@Argument String id) { return new Club(id, "Club " + id); }
  @SchemaMapping List<String> recommendations(Club c) throws Exception { Thread.sleep(1000); return List.of("b1", "b2"); }
  @SchemaMapping List<String> members(Club c) { return List.of("ada", "ben"); }

  @SubscriptionMapping
  Flux<Integer> ticks(@Argument int n, @Argument int failAt, @Argument int throwAt) {
    return Flux.interval(Duration.ofMillis(100)).take(n).map(i -> {
      int v = i.intValue() + 1;
      if (v == failAt) throw new IllegalStateException("source stream broke at " + v);
      return v;
    }).concatMap(v -> v == throwAt ? Flux.error(new RuntimeException("boom " + v)) : Flux.just(v));
  }

  // Probe: does Spring pass @defer results through? Turn the experimental flag on for every request.
  @Bean
  org.springframework.graphql.server.WebGraphQlInterceptor incremental() {
    return (req, chain) -> {
      req.configureExecutionInput((in, b) -> b.graphQLContext(c -> c.put(ExperimentalApi.ENABLE_INCREMENTAL_SUPPORT, true)).build());
      return chain.next(req);
    };
  }

  // Raw graphql-java: what does a deferred result look like?
  @RestController
  static class Raw {
    private final GraphQlSource source;
    Raw(GraphQlSource source) { this.source = source; }

    @PostMapping(value = "/raw", produces = "text/plain")
    String raw(@RequestBody Map<String, Object> body) {
      GraphQL g = source.graphQl();
      ExecutionInput in = ExecutionInput.newExecutionInput((String) body.get("query"))
          .graphQLContext(Map.of(ExperimentalApi.ENABLE_INCREMENTAL_SUPPORT, true)).build();
      long t0 = System.nanoTime();
      var r = g.execute(in);
      StringBuilder sb = new StringBuilder();
      sb.append(String.format("initial at %d ms: %s %s%n", (System.nanoTime() - t0) / 1_000_000, r.getClass().getSimpleName(), r.toSpecification()));
      if (r instanceof IncrementalExecutionResult ir) {
        Flux.from(ir.getIncrementalItemPublisher()).doOnNext(p ->
            sb.append(String.format("later at %d ms: %s%n", (System.nanoTime() - t0) / 1_000_000, p.toSpecification()))).blockLast();
      }
      return sb.toString();
    }
  }
}
