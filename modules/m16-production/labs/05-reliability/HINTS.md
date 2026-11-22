# Hints: 16.5 reliability

1. Measure before you fix: run the starter through `Peak.java` while the service is slow. Its p95 per second shows
   how long one stuck call holds a whole page. `SimpleClientHttpRequestFactory` has `setReadTimeout`. Give the
   long-poll `events()` client its own factory: it waits up to 5 s on purpose.
2. Resilience4j decorates a `Supplier`: `CircuitBreaker.decorateSupplier(breaker, call)`, and
   `Bulkhead.decorateSupplier(bulkhead, ...)` around that. A breaker counts slow calls as failures when you set
   a slow-call threshold below your timeout. With a sliding window of about 20 calls it opens within a fraction of
   a second at 100 pages a second.
3. A batch mapping that throws fails every field in that batch. Map the exception you throw to a GraphQL error
   with a `@GraphQlExceptionHandler` in a `@ControllerAdvice`, using the `DataFetchingEnvironment`, so each error
   gets its field's path. Wrap every failure (timeout, open breaker, full bulkhead, connection refused) in one
   exception type of your own.
