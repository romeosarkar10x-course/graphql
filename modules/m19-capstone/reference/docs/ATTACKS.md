# Attack report

Run on 2026-11-23 against the full stack (router, inventory, lending) inside the course image, with
`scripts/attacks.sh`, which anyone can re-run: every attack is a request, and the script says BLOCKED or OPEN.
Raw output: the course repository's `_workbench/2026-11-23-capstone/probe/attacks2.txt`.

## Results

| # | Attack | Result | Stopped by |
| --- | --- | --- | --- |
| A01 | A query that is not a persisted document (`tools(first: 50)` with barcodes) | Blocked: `PERSISTED_DOCUMENT_ID_REQUIRED` | Router, trusted documents |
| A02 | Introspection as a raw query | Blocked: same | Router, trusted documents |
| A03 | A made-up document id | Blocked: `PERSISTED_DOCUMENT_NOT_FOUND` | Router |
| A04 | An expired token | Blocked: `UNAUTHORIZED`, `me: null` | Router (logs it), lending (401) |
| A05 | A token signed with another key, claiming the course key's `kid` | Blocked: `UNAUTHORIZED` | Signature check, router and lending |
| A06 | `alg: none`, no signature | Blocked: `UNAUTHORIZED` | Both refuse the algorithm |
| A07 | A valid course token for another API (`aud: shelfie-api`) | Blocked: `UNAUTHORIZED` | Audience check |
| A08 | `OverdueReport` with `first: 50, history: 50` | Blocked: `COST_ESTIMATED_TOO_EXPENSIVE` (estimate 7,755) | Router, demand control |
| A09 | A page of 10,000 tools | Blocked: `COST_ESTIMATED_TOO_EXPENSIVE` | Router, before the resolver's own 50 limit |
| A10 | Olu returns Ada's loan `l2` | Blocked: `LoanNotFound` | Lending: someone else's loan does not exist for you |
| A11 | A member asks who borrowed the circular saw (`DeskLookup`) | Blocked: `member: null`, `FORBIDDEN` | Field rule on `Loan.member` |
| A12 | A member runs the librarians' overdue report | Blocked: `FORBIDDEN` | Field rule on `overdueLoans` |
| A13 | `%' OR 1=1 --` in the search box | Blocked: no results, no error | Bind parameters; `%` and `_` escaped in the LIKE pattern |
| A14 | A hand-made cursor with SQL in its sort key | Blocked: `BAD_REQUEST` | Cursor names its sort order; values are bind parameters |
| A15 | Olu reuses Ada's idempotency key to read her answer | Blocked: Olu gets his own answer (`ToolUnavailable`) | Keys are per member |
| A16 | A batch of 50 operations in one HTTP request | Blocked: "Failed to parse GraphQL request payload" | The router does not accept batches |
| A17 | 60 concurrent `MyLoans`, Ada's and Olu's tokens interleaved | Blocked: each got their own | The router merges identical in-flight subgraph requests (see LOAD.md), but not across different `Authorization` headers |
| A18 | Lending directly, past the router, from the container's network address | Blocked: connection refused | Subgraphs listen on 127.0.0.1 |

18 attacks, 0 open.

## Findings worth fixing anyway

- **The router's answers are all HTTP 200**, its own refusals included. Correct for GraphQL over HTTP's
  `application/json` responses, but a WAF or a rate limiter in front cannot tell a refused request from a good
  one. If one is added, it should read `errors[0].extensions.code`.
- **A bad token is reported by lending, not the router.** The router logs `jwt token error` and carries on as
  anonymous (it allows anonymous requests), so the 401 comes from lending, and a query that touches only
  inventory runs with a bad token. Nothing private is in inventory, but a client with a broken token gets no
  hint on catalogue pages. Possible fix: reject in the router when an `Authorization` header is present and
  invalid (Hive Router 0.3.9 has no such switch; a coprocessor or plugin would be needed).
- **No rate limit.** Cost limits stop one expensive request, not many cheap ones. The library's traffic does not
  need it yet; the runbook says what to watch.
- **The idempotency table grows forever** (ADR 0005).

## Not tried

Subscription floods (many open SSE streams), slow-client attacks on the stream, and anything below HTTP
(TLS, the container network). Denial of service on the database by many valid borrows is bounded by the loan
limit per member, not by the API.
