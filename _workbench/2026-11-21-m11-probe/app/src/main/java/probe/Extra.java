package probe;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.graphql.execution.SubscriptionExceptionResolverAdapter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.*;

@Controller
@RestController
@Configuration
class Extra {
  static final AtomicInteger active = new AtomicInteger(), emitted = new AtomicInteger(), cancelled = new AtomicInteger();
  static final Sinks.Many<Integer> sink = Sinks.many().multicast().directBestEffort();

  @SubscriptionMapping
  Flux<Integer> flood(@Argument int perSecond, @Argument int seconds, @Argument String mode) {
    emitted.set(0);
    Flux<Integer> f = Flux.interval(Duration.ofMillis(1)).take(seconds * 1000L)
        .flatMapIterable(t -> java.util.stream.IntStream.range(0, perSecond / 1000).boxed().toList())
        .map(x -> emitted.incrementAndGet());
    f = switch (mode) {
      case "latest" -> f.onBackpressureLatest();
      case "drop" -> f.onBackpressureDrop();
      case "sample" -> f.sample(Duration.ofMillis(250));
      default -> f;
    };
    return f.doOnSubscribe(s -> active.incrementAndGet()).doFinally(s -> { active.decrementAndGet(); if (s == SignalType.CANCEL) cancelled.incrementAndGet(); });
  }

  @SubscriptionMapping
  Flux<Integer> idle() { return sink.asFlux().doOnSubscribe(s -> active.incrementAndGet()).doFinally(s -> { active.decrementAndGet(); if (s == SignalType.CANCEL) cancelled.incrementAndGet(); }); }

  @GetMapping("/stats")
  Map<String, Object> stats() {
    Runtime r = Runtime.getRuntime();
    return Map.of("active", active.get(), "emitted", emitted.get(), "cancelled", cancelled.get(), "sinkSubscribers", sink.currentSubscriberCount(),
        "heapUsedMb", (r.totalMemory() - r.freeMemory()) >> 20);
  }

  @Bean
  SubscriptionExceptionResolverAdapter named() {
    return new SubscriptionExceptionResolverAdapter() {
      @Override protected GraphQLError resolveToSingleError(Throwable ex) {
        return GraphqlErrorBuilder.newError().message("source failed: " + ex.getMessage()).extensions(Map.of("code", "SOURCE_FAILED")).build();
      }
    };
  }
}
