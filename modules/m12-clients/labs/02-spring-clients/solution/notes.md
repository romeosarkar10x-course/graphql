# Spring's clients: notes

Reference notes (grader only), from the reference solution's run on 2026-11-22.

## Sync or reactive

`HttpSyncGraphQlClient` (RestClient) blocks the request thread until Shelfie answers: simple code, one thread
per in-flight call. `HttpGraphQlClient` (WebClient) returns a `Mono`; on Spring MVC the controller returns it
and the servlet thread is released while Shelfie works. The digest is mostly fan-in of one or two calls per
request, so sync is fine, and with `spring.threads.virtual.enabled=true` a blocked request thread is a cheap
virtual thread. Reactive pays off when one request fans out to many calls (`Mono.zip`) or the service is
WebFlux anyway. Both share documents, interceptors (in their own sync and reactive flavours) and `toEntity`.

## What toEntity did

- Unknown club: `club` is null with no errors, `toEntity` returns `null` (sync) and an empty `Mono`
  (reactive); the controller turns both into 404.
- A field that does not exist (`{ club(id: "c1") { nope } }`): `FieldAccessException: Invalid field 'club',
  errors: [{message=Validation error (FieldUndefined@[club/nope]) : Field 'nope' in type 'Club' is
  undefined, ...}]`, a 500 unless mapped. The error is a GraphQL error, not an HTTP one: Shelfie answered 200.
- userErrors (DUPLICATE) are data, so `toEntity(ReviewPayload.class)` succeeds and the controller picks 422.
- `documentName("clubDigest")` loads the file but sends no `operationName`; the interceptor saw
  `(anonymous)` until `.operationName("clubDigest")` was added.
- Shelfie returned 404 for every call at first: `@Value("${shelfie.url}")` was bound from the course image's
  `SHELFIE_URL` environment variable (relaxed binding) and lost `/graphql`.

## The subscription across a chat-server restart

While the server was down the subscription logged `GraphQlTransportException: ... Connection refused:
localhost/127.0.0.1:4100` every 200 ms to 2 s (`retryWhen` with backoff); after the restart it subscribed
again and received the next message. Everything sent between the drop and the resubscribe is lost: a
subscription delivers only events after it starts. To fill the gap, remember the last message id and query
`messages(roomId, after: lastId)` before trusting live events again (lab 11.2's rule: hold live events until
the gap query returns). Here the restarted server also reset its ids to m1, so "after m7" would be wrong
across a restart: the client would need the server's epoch or a timestamp.
