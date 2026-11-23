# Demo day: Toolshed in 10 minutes

## Script

| Minute | Show | Say |
| --- | --- | --- |
| 0-1 | The pitch's problem: two members, one pressure washer | Who uses it and the three things it must get right |
| 1-3 | The contract: `ToolPage`, `BorrowResult`, `Problem`, `ToolLookup @oneOf` | Client-first: every operation is a screen; failures are data |
| 3-4 | The UI (`client ui`) browsing as anonymous, then the CLI borrowing as Ada | One graph, two clients, persisted ids only |
| 4-5 | Borrow the same tool from the CLI as Olu: `ToolUnavailable` with the date; retry Ada's borrow with the same key | Typed failures; idempotency |
| 5-6 | The badge in the browser flips when the CLI returns the tool | The subscription over SSE through the router |
| 6-7 | `scripts/attacks.sh`: 18 BLOCKED | Trusted documents, cost, field rules, the 127.0.0.1 subgraphs |
| 7-8 | One `MyLoans` trace in Tempo: router, lending, inventory | The query plan is two fetches; the dashboard's statements-per-request panel |
| 8-9 | Stop lending; reload the catalogue | `available: null` and "ask at the desk": the nullability decision that the outage test changed |
| 9-10 | Limits: one lending instance, seed at start, no rate limit | What I would do next, in order |

## Questions to expect

- **Why a router instead of one Spring app?** ADR 0001 and 0002: two rates of change and one security edge; the
  cost is a second process and persisted documents.
- **Why Hive Router?** MIT, local file for persisted documents, demand control from `@listSize`; Apollo
  Router's safelisting needs GraphOS.
- **How do you know there is no N+1?** The statement counter, a test at 2 and 30 tools, and the dashboard panel.
- **What breaks first under load?** Not throughput (2,711/s on one machine); the single lending instance
  and its in-memory subscriptions.
- **Why is `idempotencyKey` required?** Optional keys are forgotten by exactly the clients that retry.

## Fallbacks

If the browser cannot reach the container, show the CLI and `curl` with SSE (`Accept: text/event-stream`).
If Tempo search lags (it does, 20 to 30 s), open the trace by id: the demo request sends its own `traceparent`.
