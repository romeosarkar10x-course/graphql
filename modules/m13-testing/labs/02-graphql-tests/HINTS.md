# Hints: m13/02

1. `ExecutionGraphQlServiceTester` calls `ExecutionGraphQlService` directly: no HTTP, so no
   `WebGraphQlInterceptor`. A bug in the interceptor is invisible to it; only a tester that goes through the web
   layer (`HttpGraphQlTester`, or `WebSocketGraphQlTester`) can catch it. In-process, set the viewer with
   `mutate().configureExecutionInput(...)`.
2. `errors().verify()` fails on any error you did not expect; `errors().expect(e -> ...)` filters the ones you
   did, `errors().satisfy(list -> ...)` gives you all of them with `getErrorType()`, `getPath()`,
   `getParsedPath()` and `getLocations()`.
3. The review feed is a hot multicast: an event published before the server has registered your subscription
   is gone. `StepVerifier.then(...)` runs after the client subscribes, not after the server has. Wait for
   `ReviewService.liveSubscribers()` before you publish, and bound the test with `take(Duration)` so "exactly
   three" means a fourth would have been seen.
