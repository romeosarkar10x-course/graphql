# Bake-off: one feature, four stacks

The feature: the club card, `{ club(id: "c1") { name memberCount owner { name } currentBook { title } } }`.

## Numbers (median of 3 rounds of `./bake.sh`)

Course image, 24-core build machine, 2026-11-21, default JVM flags (`_workbench/2026-11-21-m05-probe/bake-off.txt`):

| Stack | Lines | Startup ms | RSS MB | p50 ms | p95 ms | req/s |
|---|---|---|---|---|---|---|
| raw graphql-java | 146 | 303 | 584 | 0.52 | 1.08 | 13,369 |
| Spring for GraphQL | 103 | 1,109 | 607 | 0.72 | 1.57 | 9,378 |
| DGS | 49 | 1,448 | 659 | 0.78 | 1.63 | 8,843 |
| Quarkus | 69 | 519 | 518 | 0.79 | 1.42 | 9,006 |

Caveats: the apps are not feature-equal. Raw and Spring serve the whole Module 5 schema; DGS and Quarkus
serve a slice, which flatters their line counts. RSS is dominated by the default heap sizing on a big
machine (a quarter of RAM): it says little until `-Xmx` is set equally. Latency is loopback with 8 client
threads, so it measures framework overhead per request, which is under a millisecond everywhere.

## Test ergonomics

Raw: a JUnit test calling `graphQL.execute(...)` directly, milliseconds, or the Hurl suite against a
started jar. Spring: `@GraphQlTest` slice tests with `GraphQlTester` (no HTTP, ~1 s context), or
`HttpGraphQlTester` against `@SpringBootTest`. DGS: `DgsQueryExecutor` in a `@SpringBootTest` plus the
generated client classes for typed queries. Quarkus: `@QuarkusTest` with RestAssured or the typesafe
client; dev mode's continuous testing reruns them on save.

## Verdict

Spring for GraphQL for Shelfie: within 30% of raw graphql-java on throughput, one second of startup
that a long-running server never notices, the largest ecosystem (security, data, observability, used in
Modules 8 to 15), and schema-first by default. I would switch to Quarkus if Shelfie ran as
scale-to-zero functions where startup dominates (cold starts above roughly 300 ms matter there), and to
raw graphql-java only for a library or an embedded engine. DGS only if we needed its client codegen or
Netflix-style federation tooling.
