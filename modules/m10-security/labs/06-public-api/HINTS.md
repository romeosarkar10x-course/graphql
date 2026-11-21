# Hints, lab 10.6

1. Three layers: a servlet filter for the key (so a bad key is a plain 401), a `WebGraphQlInterceptor`
   that puts the key's client into the `GraphQLContext`, and instrumentation for scopes and the budget.
2. Bucket4j's leaky bucket: `capacity(maximum).refillGreedy(restoreRate, Duration.ofSeconds(1))`, one bucket
   per app. `tryConsumeAndReturnRemaining(cost)` gives you `currentlyAvailable` for the response either way.
3. Spender: keep the last response's `requestedQueryCost`, `currentlyAvailable` and `restoreRate`; before
   each request, if available < cost, sleep `(cost − available) / restoreRate` seconds. A throttled response
   carries the same numbers in `errors[0].extensions.cost`.
