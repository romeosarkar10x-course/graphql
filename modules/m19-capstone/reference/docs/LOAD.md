# Load test

`scripts/Load.java`: a mix of what members do (60% catalogue page, 20% filtered and sorted page, 10% tool page,
10% "my loans" signed in), through the router with persisted ids, on virtual threads. After a 5-second warm-up.

## Setup

One machine, 24 cores, everything inside the course container: PostgreSQL 18.7, the two subgraphs
(`-Xmx256m`), Hive Router 0.3.9 and the load generator. Seed data (30 tools). 2026-11-23.

## Results

20 seconds at concurrency 8:

| Operation | Requests | p50 ms | p95 ms | p99 ms |
| --- | --- | --- | --- | --- |
| ToolPage | 32,180 | 2.7 | 4.9 | 6.2 |
| ToolPage with filter and sort | 11,165 | 2.8 | 5.0 | 6.4 |
| ToolDetail | 5,389 | 2.3 | 4.4 | 5.4 |
| MyLoans (signed in) | 5,488 | 3.0 | 5.3 | 6.8 |

54,222 requests, 2,711 per second, 0 errors, **2.48 SQL statements per request** across both subgraphs.

## What the numbers say

- **No N+1.** Measured one request at a time: a catalogue page costs 4 statements in inventory (page,
  categories, branches, count) and 1 in lending (open loans for the page); `ToolDetail` 3 and 1; `MyLoans` 2
  and 2. The same page at 30 tools takes the same number (`InventoryGraphQlTest`, the checker's N+1 probe).
- **Why 2.48 and not about 4.8.** At concurrency 1 the same mix averages 4.81 statements per request (344
  requests per second); at concurrency 8 it is 2.47. Hive Router merges identical subgraph requests that are in
  flight at the same moment (`traffic_shaping` dedupe, on by default), and eight clients sending the same
  catalogue page often coincide. It does not merge requests with different `Authorization` headers
  (ATTACKS.md, A17). Free capacity under exactly the load that needs it; the numbers above include it.
- **Headroom.** The library expects a few hundred page views an hour on a Saturday; this is about four orders
  of magnitude more. The limit worth watching is not throughput but the single lending instance (ADR 0006).
- **The router is cheap.** p50 of 2.7 ms includes two subgraph calls and the router's cost estimate.

## Caveats

- One run, one machine, all processes sharing it; the load generator competes with the server for CPU.
- 30 tools: the keyset queries are index scans whatever the size, but a real catalogue would show it.
- Every request is a cache-cold GraphQL execution; the router does not cache responses.
