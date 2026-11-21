# Transports: notes (reference)

Runs from 2026-11-21: `_workbench/2026-11-21-m11-probe/lab02-frames.txt` and `lab02-reconnect.txt`.

## The same subscription on both transports

`subscription { messageSent(roomId: "r1") { id author body } }`, two messages sent 0.3 s apart.

WebSocket (`graphql-transport-ws` subprotocol, one socket; `Ws.java` prints each frame's payload):

```
client: {"type":"connection_init"}
server: {"id":null,"payload":{},"type":"connection_ack"}
client: {"id":"1","type":"subscribe","payload":{"query":"subscription { ... }"}}
   520 next {"data":{"messageSent":{"id":"m7","author":"Ada","body":"one"}}}
   832 next {"data":{"messageSent":{"id":"m8","author":"Ben","body":"two"}}}
  5003 ping                      (keep-alive after 5 s of silence; the client answers pong)
```

SSE (one `POST /graphql` with `Accept: text/event-stream` per subscription):

```
HTTP/1.1 200
Content-Type: text/event-stream
Transfer-Encoding: chunked

event:next
data: {"data":{"messageSent":{"id":"m7","author":"Ada","body":"one"}}}

event:next
data: {"data":{"messageSent":{"id":"m8","author":"Ben","body":"two"}}}
```

The WebSocket multiplexes: one socket carries many operations, each tagged with an `id`, and needs its own
handshake and protocol (`connection_init`, `ping`/`pong`). SSE is plain HTTP: every subscription is its
own response, which proxies, load balancers and HTTP auth already understand, but each costs a connection
(and HTTP/1.1 browsers allow six per host). Surprise: Spring commits the SSE response headers only with
the first event or keep-alive, so a client cannot use "headers received" to mean "subscribed". The
client waits 300 ms instead.

## The drop

The balancer closed every connection and refused new ones for 1.5 s. WebSocket: `SocketException:
Connection reset`. SSE: `IOException: closed` on the next read. Neither transport resumes anything by
itself: graphql-transport-ws has no resume, and Spring's SSE sends no `id:` lines for a `Last-Event-ID`
reconnect. The client's log:

```
connected ws
live m9
dropped: java.net.SocketException: Connection reset
reconnecting in 202 ms
dropped: ... HTTP/1.1 header parser received no bytes
reconnecting in 420 ms
dropped: ... Connection reset by peer (connect failed)
reconnecting in 818 ms
dropped: ... Connection reset by peer (connect failed)
reconnecting in 2332 ms
connected ws
filled 2
```

stdout was `m9 Ada: before`, `m10 Ben: during`, `m11 Cleo: after`: each once, in order. "Subscribe, then
query the gap" cannot lose a message: anything sent after the subscription starts arrives live, anything
before is in the query, and the overlap is removed by id. "Query, then subscribe" loses whatever is sent
between the two. One more race, found by game 11.10's stress test (`_workbench/2026-11-21-m11-probe/stress1/`): the new
subscription delivered `m37` before the gap query ran, the client printed it, and the gap query then
asked for messages after `m37` and never saw `m36`. Live messages must be held from the moment the new
subscription is sent, not from the moment the gap query starts. The jitter (backoff plus up to half again) keeps a thousand clients cut off at once from
coming back in one wave.

## Keep-alives

`spring.graphql.websocket.keep-alive: 5s` sends a graphql-transport-ws `ping` after 5 s of silence;
`spring.graphql.http.sse.keep-alive: 5s` sends an SSE comment line (`:`). Proxies and load balancers close
connections that look idle (often after 60 s), and a connection whose other end vanished can look open
for a long time; a keep-alive either keeps it open or makes the write fail, so the dead connection is
noticed.

## RSocket and multipart HTTP

RSocket is a binary protocol with streams, backpressure (`request(n)`) and resumption built in; Spring for
GraphQL serves it with `GraphQlRSocketHandler`, and Java clients use `RSocketGraphQlClient`. It suits
service-to-service streams on the JVM, not browsers. Multipart HTTP subscriptions (Apollo's protocol: one
`multipart/mixed` response whose parts are events) are spoken by Apollo Client and Apollo Router; Spring
for GraphQL 2.1 does not serve them (`Accept: multipart/mixed` alone gets 415).
