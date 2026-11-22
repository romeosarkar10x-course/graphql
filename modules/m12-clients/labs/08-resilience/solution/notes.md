# Client resilience: notes

Reference notes (grader only), from four runs of the reference solution on 2026-11-22
(`_workbench/2026-11-22-m12-probe/resilience-runs.txt`).

## Before and after

```
plain:     ok 143/200, failed 57, circuit open 0, 4410 ms
resilient: ok 199/200, failed 1, circuit open 0, 9545 ms
unreachable: ok 0/100, failed 2, circuit open 98, 854 ms (wall 1461 ms)
```

Over four runs plain was 65–71% and resilient 99–100%. Each call meets the `reset_peer` toxic (30%, the
response is cut after Shelfie answered) and the `timeout` toxic (5%, the connection hangs until the 500 ms
read timeout): 0.7 × 0.95 ≈ 66%. Five attempts fail together with about 0.34^5 ≈ 0.5% chance, so 1 failure
in 200 is expected. Toxiproxy picks toxics per *connection*: with HTTP keep-alive the client reused one healthy
connection and a "30%" toxic hit 1 request in 100 (`toxi-probe.txt`); with `-Djdk.httpclient.keepalive.timeout=0`
every call opens a connection and the rate is real. Real networks fail per connection too: a pooled
connection that has gone bad fails every request on it.

## What is safe to retry

Queries, `joinClub` and `leaveClub` (idempotent by contract, lecture 4.4) can be retried blindly.
`createReview` cannot: a lost response does not mean the review was not stored (probe: Shelfie stored all
10 while the client saw 4 failures). The reviews command retries it, and when a retry after an unknown
outcome answers `DUPLICATE`, it looks up the existing review and reports `recovered` with its id (3 to 6 of
10 per run). That works only because Shelfie's rule "one review per user and book" makes the second attempt
detectable. In general the server needs an idempotency key: the client sends a key per intent
(`Idempotency-Key` header or an input field such as `clientMutationId`), the server stores the first result
under it and replays it for repeats.

## The breaker

Sliding window of 20 calls, at least 10 before deciding, open at 80% failures, half-open after 5 s. With 35%
of calls failing at the network the breaker must stay closed (retries fix that); at 80% Shelfie is down.
Retry is the outer decorator, so every attempt is recorded and `CallNotPermittedException` is not retried:
the 100 calls to an unreachable Shelfie took 1.5 s instead of 100 × 5 attempts with backoff. Without a breaker
every client multiplies its traffic by the attempt count exactly when the server is weakest, and a server
coming back up is flattened by the queued retries (retry storm); jitter spreads them.

## Persisted documents from the client

With APQ the client first sends only `extensions.persistedQuery {version: 1, sha256Hash}`; a server that does
not know the hash answers `PersistedQueryNotFound`, and the client resends with the full `query` and the hash.
After a server restart (an in-memory store) the first call of every client gets `PersistedQueryNotFound`. That
is an answer, not a transport failure: the client must resend with the document at once and not count it
against the breaker or retry it with backoff. With a trusted-documents allow-list (lecture 10.5) there is no
second step: the build registers the documents, and an unknown hash is a client bug.
