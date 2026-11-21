# Transport and WebSocket security: notes (reference)

## The hostile-origin socket, before and after

Before (the starter, `_workbench/2026-11-21-m10-probe/lab07-starter-run.txt`): a socket from
`Origin: http://evil.test` was accepted, `connection_init` was acknowledged, and the subscription ran.
It only failed with "not a member of c1" because the token in the payload was ignored, so even ada was
anonymous on the socket (W02 failed the same way), and a forged token was acknowledged too (W04). Why
evil.test got in at all: Spring refuses a cross-origin WebSocket handshake by default ("Rejecting
cross-origin WebSocket request"), unless CORS allows the origin, and Boot hands `spring.graphql.cors` to
the WebSocket handler. With no CORS config only same-origin sockets open; with the starter's
`allowed-origin-patterns: "*"` every origin did (`_workbench/2026-11-21-m10-probe/ws-origin-probe.txt`).
One CORS line fixed both. After: the handshake from evil.test is a 403 before any GraphQL message, ada's token in `connection_init` makes her the security context of her subscription
(W02 received "live note"), and a forged token closes the socket with 4401.

## Why a WebSocket needs its own authentication step

A browser cannot set an `Authorization` header on a WebSocket handshake, so a bearer token cannot ride on
the upgrade the way it does on every POST. graphql-transport-ws puts it in `connection_init`'s payload
instead; the server verifies it once per socket (`AuthenticationWebSocketInterceptor` with the same
`JwtDecoder` as HTTP) and closes on failure. Two consequences: the token is checked at connect time only,
so a socket outlives its token's `exp` unless the server closes it; and cookies *are* sent on a
cross-site upgrade, which is why a cookie-authenticated socket without an origin check is cross-site
WebSocket hijacking.

## What the log and the error used to give away

The request log wrote every header, so every bearer token (and API key) was in the log: anyone with log
access could replay them for 5 minutes, and logs live far longer and travel further (aggregators, support
tickets). Now `Authorization`, `Cookie`, `X-Api-Key` are `[redacted]`. The error handler returned the
exception message to the client: `recommender http://recs.internal:9000/v2 refused first=11 for api key
sk_live_7f3a9c2e1b (limit 10)`, an internal host, a live secret and the limit. Now the client gets
`INTERNAL_ERROR` and the execution id (Spring for GraphQL's default for unresolved exceptions), and the
message is in the server log with the same id.

## CORS: what it protects and what it does not

CORS decides which origins' JavaScript may *read* responses (and, in Spring, which may open a socket),
and the starter allowed every origin *with credentials*, reflecting the caller's origin back: any site could read a signed-in user's data where the
browser holds cookies. Now only `https://shelfie.test`, no credentials. CORS does not stop requests: a
CORS-simple request (lab 10.3's `text/plain` form) is still sent and still executes; only reading the
answer is blocked. It also does nothing for non-browser clients. Server-side rules (content types, CSRF
tokens or no cookie auth, origin checks on sockets) do the protecting.
