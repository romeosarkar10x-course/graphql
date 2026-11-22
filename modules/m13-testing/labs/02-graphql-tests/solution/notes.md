# Lab 13.2 notes

## 1. Which tester saw which bug?

Mutant G6 (the interceptor reads the wrong header) survives every `ExecutionGraphQlServiceTester` test: that
tester calls `ExecutionGraphQlService` directly, so `WebGraphQlInterceptor`s never run and my in-process
tests set the viewer on the execution input themselves. Only `OverHttpTest` (HttpGraphQlTester over MockMvc)
failed, all three of its header tests. The error mutants (G4, G5) and the
userErrors mutant (G7) were caught in-process, which is the cheapest place to catch them.

## 2. How do you make "exactly 3 events, in order" not flaky?

The feed is a hot multicast sink: an event published before the server registers the subscription is lost.
`StepVerifier.then(...)` runs after the client subscribes, not after the server does, so the test waits on
`ReviewService.liveSubscribers()` before it publishes. A sleep would also work most of the time, and fail on
a slow CI machine. The test publishes a fourth review to another book to prove the filter, takes events for
3 seconds and expects exactly three, then completion: an extra event or a missing one both fail.

## 3. Path assertions against the whole response

`path("club.members[*].handle")` pins only what the test cares about, so adding a field to the document
does not break it. Comparing the whole JSON would catch more (an unexpected extra error, say), but every
schema addition would rewrite expected files. `errors().verify()` is the middle ground: it fails on any
unexpected error even when the test checks just one path.

## 4. DgsQueryExecutor against Spring's testers

`executeAndExtractJsonPath` is one call and returns plain maps and lists; Spring's testers read typed
entities and fail with a path in the message. DGS's executor has no WebSocket or HTTP flavour: for those
DGS 12 runs on Spring for GraphQL, so the same Spring testers apply.
