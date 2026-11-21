package probe;

import java.time.Duration;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

@Controller
class Ticked {
  record Tick(int n, int badAt) {}

  @SubscriptionMapping
  Flux<Tick> ticked(@Argument int n, @Argument int badAt) {
    return Flux.interval(Duration.ofMillis(100)).take(n).map(i -> new Tick(i.intValue() + 1, badAt));
  }

  @SchemaMapping
  Integer risky(Tick t) {
    if (t.n() == t.badAt()) throw new IllegalStateException("risky failed at " + t.n());
    return t.n() * 10;
  }
}
