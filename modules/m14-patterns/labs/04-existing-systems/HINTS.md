# Hints: m14/04

1. `mvn compile` generates `dev.shelfie.bff.grpc.v1.ShelfieServiceGrpc` and the message classes. A
   `ManagedChannelBuilder.forTarget(target).usePlaintext().build()` and `ShelfieServiceGrpc.newBlockingStub` are
   all the client needs; give every call `withDeadlineAfter`, as you gave REST a timeout.
2. Without a read timeout the JDK HTTP client waits for as long as Open Library likes. `JdkClientHttpRequestFactory`
   takes an `HttpClient` with a connect timeout and has `setReadTimeout`. Every failure (timeout, 5xx, a body
   that is not JSON) should become one exception type that a `@GraphQlExceptionHandler` maps to the error the
   schema describes.
3. Resilience4j's `CircuitBreaker.of(name, config)` with a small sliding window, a minimum number of calls and a
   short open state; `executeSupplier` throws `CallNotPermittedException` while open: that is "unavailable" too.
   A record called `BookDto` maps to GraphQL type `BookDto` unless the mapping names `typeName`.
