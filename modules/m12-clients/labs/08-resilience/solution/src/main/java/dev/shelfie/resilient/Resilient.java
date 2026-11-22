package dev.shelfie.resilient;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Retry outside, circuit breaker inside: every attempt is counted by the breaker, and once it is open the
 * retry gives up at once instead of waiting out its backoff against a dead server.
 */
public class Resilient {
  private final CircuitBreaker breaker = CircuitBreaker.of("shelfie", CircuitBreakerConfig.custom()
      .slidingWindowSize(20)
      .minimumNumberOfCalls(10)
      .failureRateThreshold(80)                  // 30% failures are weather; 80% is an outage
      .waitDurationInOpenState(Duration.ofSeconds(5))
      .recordExceptions(ResourceAccessException.class, HttpServerErrorException.class)
      .build());

  private final Retry retry = Retry.of("shelfie", RetryConfig.custom()
      .maxAttempts(5)
      .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(Duration.ofMillis(25), 2.0, 0.5, Duration.ofMillis(400)))
      // Only "no answer" and 5xx. HttpSyncGraphQlClient throws RestClient's own exceptions here (an I/O error is a
      // ResourceAccessException), not GraphQlTransportException. GraphQL errors and userErrors are answers.
      .retryExceptions(ResourceAccessException.class, HttpServerErrorException.class)
      .ignoreExceptions(CallNotPermittedException.class)
      .build());

  /** For idempotent calls (queries): any transport failure may be retried. */
  public <T> T call(Supplier<T> call) {
    return Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(breaker, call)).get();
  }

  /** One attempt through the breaker only, for callers that decide themselves whether a retry is safe. */
  public <T> T once(Supplier<T> call) {
    return CircuitBreaker.decorateSupplier(breaker, call).get();
  }

  public long backoffMillis(int attempt) {
    return Math.min(400, 25L << attempt);
  }
}
