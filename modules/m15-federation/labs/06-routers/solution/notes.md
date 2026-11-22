# Lab 15.6 notes

## Same traffic, two routers

2000 requests of one query that touches all four subgraphs (reviews, then accounts and catalog, then clubs for
`weeksToRead`), 16 at a time, everything in one course container:

| router | p50 | p95 | p99 | max | memory (RSS after) | errors |
|---|---|---|---|---|---|---|
| Apollo Router 2.19.0 | 5 ms | 16 ms | 29 ms | 40 ms | 290 MB | 0 |
| Hive Router 0.3.9 | 3 ms | 5 ms | 12 ms | 19 ms | 151 MB | 0 |

Same plans (both made `[reviews], [accounts, catalog], [clubs]`), same answers. Hive was faster at the tail and
used half the memory here. What this does not show: the subgraphs, the load generator and the router shared
the CPUs, the subgraphs are in-memory JVMs that answer in about a millisecond, and nothing was cached or warmed
for long. With real subgraphs at 20-50 ms each, the router's few milliseconds are a small part of the page; the
number of sequential steps in the plan matters more than which router runs it.

## Config: same rules, two dialects

| rule | Apollo Router | Hive Router |
|---|---|---|
| forward credentials | `headers.all.request.operations: [propagate: {named: ...}]` (2.19 moved the list under `operations`, and rewrites the old form with a warning) | `headers.all.request: [propagate: {named: ...}]` |
| introspection off | `supergraph.introspection: false` | `introspection: false` |
| subgraph timeout | `traffic_shaping.all.timeout: 2s` | `traffic_shaping.all.request_timeout: 2s` |
| query plans hidden | default (plugin `experimental.expose_query_plan` off) | default (`query_planner.allow_expose: false`) |
| where the config comes from | `--config` | `ROUTER_CONFIG_FILE_PATH` or `./router.config.yaml`; `--config` is silently ignored |

The timeout errors differ: Apollo says "Subgraph errors redacted" unless `include_subgraph_errors` is on, Hive
names `SUBGRAPH_REQUEST_TIMEOUT` and the service. And the field must be nullable for the rest to survive: the
same timeout on `reviewCount: Int!` nulled the whole `book`.

## What would decide it for Shelfie

Licences: Apollo Router's subscriptions, JWT authentication, `@authenticated` and request limits need a GraphOS
plan (it refused to start with `subscription.enabled: true`); Hive Router is MIT and has subscriptions, but
0.3.9 rejects `@defer` ("Unknown directive"), which Apollo Router serves without a licence. Both hot-reload a
supergraph file. Hive Gateway (Node.js), WunderGraph Cosmo Router (Go) and Hot Chocolate Fusion (.NET) are the
other choices; Nadel and feddi run on the JVM, which matters to a team whose policy code is Java.
