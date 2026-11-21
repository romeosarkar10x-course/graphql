# Hints: m11/06

1. Two parts: decide per request whether to enable incremental support at all, and add an HTTP endpoint
   that can write a response in several flushes. Spring's own `/graphql` route answers first unless yours
   is ordered before it.
2. A `RouterFunction<ServerResponse>` bean annotated `@Order(Ordered.HIGHEST_PRECEDENCE)`, matching
   `POST /graphql` and an Accept header that names multipart/mixed, can call the `WebGraphQlHandler` bean
   (so interceptors still run) and get the `ExecutionResult` from the response. If it is an
   `IncrementalExecutionResult`, `getIncrementalItemPublisher()` gives the later payloads.
3. `ServerResponse.ok().contentType(...).build((req, res) -> { ... return null; })` hands you the servlet
   response: write `"\r\n---\r\nContent-Type: application/json; charset=utf-8\r\n\r\n"`, the payload's
   `toSpecification()` as JSON, and flush, for each part; finish with `"\r\n-----\r\n"`. Match
   `multipart/mixed` by type and subtype, not with `isCompatibleWith` (which `*/*` passes).
