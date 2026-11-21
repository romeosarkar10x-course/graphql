# Lab 10.7: Transport and WebSocket security

Lecture: [10.7 Transport and WebSocket security](../../lectures/10.7-transport-websocket.md). Grading: auto + rubric. Needs nothing.

The starter is lab 10.2's solution with subscriptions over WebSocket (`/graphql`, graphql-transport-ws) and
three wire-level mistakes in `Transport.java` and `application.yaml`:

1. The token in `connection_init`'s payload is ignored: every socket is anonymous, a forged token is acknowledged.
2. CORS allows every origin with credentials, and so (find out why) any origin may open a socket.
3. The request log writes every header, and resolver exceptions reach the client verbatim.

Fix them: authenticate on `connection_init` (Spring for GraphQL's `AuthenticationWebSocketInterceptor`),
allow only `https://shelfie.test`, without credentials, both for CORS and for socket handshakes (403 for any
other origin), redact credentials in logs, and mask internal errors (keeping the detail in the
server log).

```bash
bin/course start m10/07
bin/course test m10/07
# the experiment, inside bin/course shell, with the app running:
java /course/modules/m10-security/labs/07-transport-websocket/tests/WsProbe.java ws://localhost:4000/graphql \
  http://evil.test "$(bin/course token ada)" c1 "$(bin/course token olu)"
```

`tests/WsProbe.java` opens a socket from an origin, sends `connection_init` with a token, subscribes to
`reviewAdded(clubId)`, adds a review over HTTP, and prints what happened.

## Done when

`bin/course test m10/07` passes (5 socket scenarios, 3 Hurl checks, the log) and `notes.md` passes the
rubric (`bin/course grade m10/07`).
