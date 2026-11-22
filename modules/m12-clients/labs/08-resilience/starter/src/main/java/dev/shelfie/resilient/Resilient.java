package dev.shelfie.resilient;

import java.util.function.Supplier;

/**
 * Lab 12.8: wrap Shelfie calls with Resilience4j (resilience4j-retry and resilience4j-circuitbreaker are in
 * the pom). Today both methods make exactly one attempt.
 */
public class Resilient {

  /** For idempotent calls (queries): retry what may be retried, through a circuit breaker. */
  public <T> T call(Supplier<T> call) {
    return call.get(); // TODO
  }

  /** One attempt through the breaker only, for callers that decide themselves whether a retry is safe. */
  public <T> T once(Supplier<T> call) {
    return call.get(); // TODO
  }
}
