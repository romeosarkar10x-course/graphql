# Module 11 probe findings (2026-11-21)

Spring Boot 4.2.0, Spring for GraphQL 2.1.0, graphql-java 26.1, Reactor 2026.0.0, Lettuce 7.8.0,
Apache Kafka 4.4.0 (client and broker), Spring for Apache Kafka 4.2.0, Debezium 3.7.1.Final, PostgreSQL 18.7.
Raw output in this folder; the probe app is `app/`.

## Subscriptions and their errors (lab 11.1)

1. **A source stream error over graphql-transport-ws** becomes an `error` message with
   `[{"message":"Subscription error","locations":[],"extensions":{"classification":"INTERNAL_ERROR"}}]` and no
   `complete`, the same for an exception in `map` and for `Flux.error`. A `SubscriptionExceptionResolverAdapter`
   replaces it (message and extensions of your choice). (`probe1.txt`, `probe2.txt`)
2. **Over SSE the same failure** is `event:next` with `{"errors":[...]}`, then `event:complete`. (`probe1.txt`)
3. **A field error inside one event** arrives as `next` with `data` (field null) and `errors`; the
   subscription continues and completes normally. (`probe4-field-error.txt`)
4. **Two root fields** are refused at validation (`SubscriptionMultipleRootFields`), delivered as `next` with
   `errors` then `complete`, not as a graphql-transport-ws `error`. **`@skip(if: false)` on the root field is
   accepted** (RFC 860 would forbid it). (`probe1.txt`)
5. A subscription over plain HTTP without `Accept: text/event-stream` is 200 with
   `"Operation type 'SUBSCRIPTION' is not allowed for this request"` (`OperationNotSupported`). (`probe1.txt`)

## Transports (lab 11.2)

6. **SSE needs no configuration**: Boot serves it on `/graphql` for `Accept: text/event-stream`;
   `spring.graphql.http.sse.methods: GET, POST` also allows GET. Keep-alive is an SSE comment line (`:`). (`probe2.txt`)
7. **Spring commits the SSE response headers only with the first event or keep-alive**: a Java
   `HttpClient.send` waited 2.2 s for headers, until the first message. (`SseLines.java` run, lab 11.2 notes)
8. Java's HttpClient over `http://` defaults to trying HTTP/2 (an h2c upgrade). The lab clients set HTTP/1.1;
   that change alone did not make the SSE client work (finding 7 was the cause), so it is a precaution, not
   a measured fix.
9. `spring.graphql.websocket.keep-alive: 2s` sends `{"type":"ping"}` every 2 s of silence. No `id:` lines in
   Spring's SSE, so `Last-Event-ID` resume is impossible. (`probe2.txt`, `lab02-frames.txt`)
10. **Reconnect race** (found by game 11.10): a resubscribed WebSocket delivered `m37` before the gap
    query ran; printing it moved "last seen" past `m36`, which was lost. Live events must be held from the
    moment of resubscribing. (`stress1/`)

## Backpressure and cleanup (lab 11.3)

11. **Spring's WebSocket handler applies backpressure**: a `Flux.interval` source flooding 10,000 events/s to
    a client reading 10 frames/s overflowed after about 44,600 events and ended (no log line). (`probe3.txt`)
12. **With keep-alive on and a stalled reader**, Spring logged `IllegalStateException: The remote endpoint was
    in state [TEXT_PARTIAL_WRITING]` and closed the session. (`probe2.txt`)
13. A buffered multicast sink plus a slow reader: the fast reader got all 30,000 events in 3 s, the slow one
    was still reading stale events at 12 s (backlog about 25 minutes at 20/s); RSS stayed flat. With
    250 ms windows per author: 39 events each, done at 3.1 s. (`lab03-flood.txt`)
14. A client that aborts its socket cancels the subscription (`doFinally` sees `CANCEL`; `doOnComplete` never
    runs). (`probe2.txt`)

## Scaling (lab 11.4)

15. **One Kafka consumer group for three instances**: 3 of 9 subscribers got all 9 messages, 6 got none (all
    of a room's events share a key, so one partition, so one group member). One group per instance fixed
    it. A rerun's groups resume from committed offsets and replay old events, so the test tags each run.
    (`bin/course test m11/04 --starter`)

## CDC and load (lab 11.5)

16. **Debezium 3.7.1's embedded engine runs on the Boot BOM's Kafka 4.4.0 libraries**; with `pgoutput`,
    `snapshot.mode=no_data` and in-memory offsets it streamed inserts within seconds of startup. Its default
    `poll.interval.ms` (500) showed as a 614 ms median delay to subscribers; 50 ms gave 52 ms.
    (`lab05-load-poll500ms.txt`, `lab05-load.txt`)
17. **500 clients, one container**: polling every 2 s was 250 requests/s and ~1 s median delay in a busy and a
    quiet room; CPU 8.8 s (busy, 20 s) and 12.6 s (quiet, 30 s). Subscribed: 7.9 s busy; quiet 8.3 s on a
    first run and 4.2 s warm, of which 1.7 s to open the 500 sockets. (`lab05-load*.txt`)

## Incremental delivery (lab 11.6)

18. **graphql-java 26.1 implements `@defer` only** (`@stream` is `UnknownDirective`), behind
    `ExperimentalApi.ENABLE_INCREMENTAL_SUPPORT`; payloads are `{data, hasNext}` then
    `{hasNext, incremental: [{path, label, data}]}`: no `pending`, `id` or `completed` (RFC 1110 requires a
    non-empty `pending` in the initial payload). With the flag off, `@defer` runs inline. (`probe1.txt`, `lab06-defer.txt`)
19. **Spring for GraphQL 2.1 with the flag on** returned only the initial payload (`hasNext: true`) to a JSON
    client and dropped the deferred part; `Accept: multipart/mixed` alone is 415 (with `application/json` also
    listed, plain JSON). A `@RestController` on `/graphql` never wins over Spring's router; a `RouterFunction`
    bean with `@Order(HIGHEST_PRECEDENCE)` does. `MediaType.isCompatibleWith` lets `*/*` match multipart/mixed.
    (`probe1.txt`, `probe3.txt`, `lab06-defer.txt`)
20. Club page over multipart: first byte 0.068 s, total 1.58 s; JSON with `@defer`: 1.51 s for both. (`lab06-defer.txt`)
