# Subscriptions: notes (reference)

## One chat message, from mutation to every subscriber

```
 Ada's client                 server (one JVM)                                  subscribers to r1
 ------------                 ----------------                                  -----------------
 POST /graphql  ──►  sendMessage(r1, Ada, "hi")
                       │ store.add(...)            -> m7
                       │ bus.publish(m7)  ──────►  Sinks.Many<Message>  (one emit)
                       │                              │
 { data: { sendMessage │                              ├─ filter roomId == r1 ─► source stream ─► execute
   { id: "m7" } } } ◄──┘                              │   (Ben's subscription)    selection set ─► next ─► Ben
                                                      ├─ filter roomId == r1 ─►  ... ──────────────► next ─► Cleo
                                                      └─ filter roomId == r2 ─►  (dropped: Dev watches r2)
```

The mutation's response and the subscription events are separate: the sender gets its answer over HTTP
whether or not anyone listens, and each subscriber's event is its own execution of its own selection set
against `m7` (the response stream), so Ben can ask for `{ body }` and Cleo for `{ id author body }`.

## What the client received when the source stream broke

`countdown(from: 5, failAt: 2)` over graphql-transport-ws (`java Ws.java`, 2026-11-21):

- Without a `SubscriptionExceptionResolver`: `next 5`, `next 4`, `next 3`, then
  `error [{"message":"Subscription error","locations":[],"extensions":{"classification":"INTERNAL_ERROR"}}]`.
  No `complete`: under graphql-transport-ws, `error` ends the operation. The cause is only in the log.
- With the resolver: the same three `next` messages, then
  `error [{"message":"countdown source failed at 2 (failAt: 2)", ..., "extensions":{"code":"SOURCE_STREAM_FAILED","retryable":true,...}}]`.
- Over SSE (same server): `event: next` with `{"errors": [...]}` as its data, then `event: complete`. The
  transport decides the shape: the WebSocket protocol has a dedicated `error` message, SSE has none.

An error inside one event's selection set (a field resolver throwing) is different: that event arrives as
`next` with `data` and `errors`, and the subscription goes on. Only a source stream error ends it. RFC 1127
proposes saying so in the spec.

## Why it matters

A chat client that sees "Subscription error" cannot tell "the bus is down, retry in a second" from "you
asked for a room that does not exist, stop". The `code` and `retryable` extensions give it that answer.
