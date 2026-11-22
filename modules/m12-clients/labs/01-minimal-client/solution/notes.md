# A minimal client: notes

Reference notes (grader only). The reference `ShelfieClient.java` is 181 lines, about 110 of them code.

## What you had to write by hand

| Concern | Lines (about) | What it involved |
|---|---|---|
| Transport | 20 | one `HttpClient` (HTTP/1.1, connect timeout), POST, `Content-Type: application/json`, `Accept: application/graphql-response+json, application/json`, a per-request timeout |
| Documents | 10 | load `documents/<name>.graphql` once, send `operationName` = the file name |
| Variables | 5 | a JSON object next to `query`, never pasted into the document text (injection, and the server can cache one parsed document) |
| Decoding | 25 | read `data` and `errors`; walk a dotted path; `treeToValue` into a record; null field gives null |
| Errors | 30 | three kinds: transport (no response), HTTP status (400 with a GraphQL body, 502 with HTML), and GraphQL `errors` next to partial data. `execute` returns them, `field` throws them with the partial data attached |
| Dedupe | 25 | a map of in-flight futures keyed by document + variables with sorted keys; removed when the response arrives |

Not done, and a library does: a normalised cache (lab 12.5), retries with backoff and a circuit breaker
(lab 12.8), per-call headers and auth refresh, subscriptions over WebSocket/SSE, file uploads, persisted
documents (sending a hash first), batching, code generation or typed documents, cancellation, logging and
metrics, and handling `@defer` responses.

## The case for a library

Never again: decoding (paths, typed results, error mapping) and subscriptions, because every client gets them
slightly wrong in a different way; and the cache, which is the hardest part (12.5). Fine to keep hand-written
in a small service: transport and one or two documents, where a dependency costs more than 100 lines.

Dedupe can be wrong when two calls look identical but are not: the same document and variables sent with
different headers (two users' tokens) share one response and leak data across users, so the key must include
anything that changes the answer. A mutation must never be deduplicated: two identical `createReview` calls
are two intentions (or an accidental double click the server should reject, not the client hide).
