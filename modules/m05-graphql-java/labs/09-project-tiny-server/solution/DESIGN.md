# DESIGN.md — the tiny server

## Decisions

1. **Structure.** One class: `main` (schema, exception handler, HttpServer), `wiring()` (one line per
   field, lambdas over `Data`), `createReview`, and `serve` with small helpers for negotiation and status.
   121 counted lines. Left out: authentication beyond a trusted header (Module 10), depth and cost limits
   (Module 10), DataLoader batching (Module 8), persisted queries and a document cache (5.8, Module 9),
   metrics and tracing (Module 15), request-size limits and timeouts (Module 10), subscriptions (Module 11).
2. **Status codes.** `int status = !result.containsKey("data") ? (parses(query) ? 422 : 400) :
   !result.containsKey("errors") || type.equals(JSON) ? 200 : 294;` The hardest rule was "data and errors"
   for legacy clients: 294 is only meaningful with `application/graphql-response+json`, so a client that
   asked for `application/json` gets 200, while a request error still gets its 4xx in either media type.
3. **Errors.** The client sees `"Internal error"` with `path` and `locations` so it knows which field
   failed; the log line has the path and the full exception. The masked message is the default for
   everything because this server has no domain exceptions yet; 5.6's typed mapping would slot into the
   same handler.
4. **Threads.** `Executors.newVirtualThreadPerTaskExecutor()`: one virtual thread per request, so 1,000
   slow requests park 1,000 cheap threads rather than queueing behind a small pool. Fetchers are
   synchronous, so inside one request siblings still run one after another (5.3); the bottleneck moves
   to whatever the fetchers call.
5. **Compared with lab 5.4.** The negotiation and status logic is the same idea, compressed; the GET
   parsing and request-shape checks were rewritten as pattern-matching `instanceof` chains. In a real
   project I would keep `GraphQLHttp` separate from the wiring: the HTTP layer is reusable across
   schemas and testable without a schema, and the wiring changes every week.
