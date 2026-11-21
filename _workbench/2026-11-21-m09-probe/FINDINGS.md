# Module 9 probe findings (2026-11-21)

Versions: `versions.txt`, `boot-bom.txt`. Data: `modules/m09-performance/contract/ClubData.java`, scale 1.

1. **Naive dashboard: 1,250 statements** (`naive-vs-batch.txt`), exactly 1 + 10 owner + 9 currentBook +
   10 ownerPicks + 10 memberCount + 10 members + 200 reviews + 1,000 books. 185-250 ms warm, 640 ms cold.
   Hibernate `show-sql` printed the same number of `Hibernate:` lines.
2. **Batched (BatchLoaderRegistry + @BatchMapping): 8 statements**, byte-identical `data`, 44-57 ms warm.
   The owner's reviews (ownerPicks, level 2) and the members' reviews (level 3) went out in one batch,
   because chaining delays the dispatch (`naive-vs-batch.txt`).
3. **Chained loaders hang by default.** graphql-java 26.1 dispatches per level; a `load` made in
   `thenCompose` after the level was dispatched is never dispatched when the batch function is
   asynchronous (`Chain.java`, `chain.txt`: default TIMED OUT; `setEnableDataLoaderChaining` or
   `setEnableDataLoaderExhaustedDispatching` both work). With `completedFuture` loaders the default
   works, so in-memory tests hide it. Spring for GraphQL 2.1.0 and Boot 4.2.0 never set either key
   (grep of both jars); in the Spring app `{ clubs(first: 2) { ownerPicks { title } } }` got no response
   in 10 s (`nochain.txt`). The fix is a `WebGraphQlInterceptor` that sets the key in the GraphQLContext.
4. **Spring for GraphQL serves POST only by default**: `spring.graphql.http.methods` defaults to `POST`
   (Boot 4.2.0 metadata); GET gets 405 (`get.txt`). With `GET,POST`, a GET mutation is 405 with
   `Allow: POST,GET` (`apq.txt`) - the Allow header lists GET although GET was just refused.
5. **APQ without a provider**: hash-only request fails as `Invalid syntax with offending token
   'PersistedQueryMarker'` (`get.txt`).
6. **APQ with `ApolloPersistedQuerySupport`** (`apq.txt`): unknown hash -> `PersistedQueryNotFound` with
   HTTP 200; query + hash registers; a mismatched hash on registration -> `PersistedQueryIdInvalid`.
   **A known hash sent with different query text runs the cached document and ignores the text**
   (club 2 asked, club 1 returned): the hash is checked only on a miss (`PersistedQuerySupport`,
   `InMemoryPersistedQueryCache.compute` returns the cached entry, sources in `gj-src/`).
7. **Spring sends no Cache-Control on GraphQL GET responses.** Varnish 9.1.0's built-in VCL then caches
   them for its default TTL (120 s): HIT on the second GET, and **HIT for a request with a different
   `X-Shelfie-User`** (`apq.txt` 6c). POST is never cached (6b).
8. Bytes: the dashboard as a POST body is 271 bytes; the APQ GET URL with only the hash is 164.
9. Varnish 9.1 VCL has no `if()` expression function; use an `if` statement.
10. **One statement is not automatically faster** (`compare.txt`, 100 dashboards after 20 warm-ups):
    naive 1,250 statements median 43.6 ms p95 54.9; batched 8 statements 11.6 / 17.3; one jOOQ
    MULTISET statement 15.2 / 19.3. jOOQ 3.21.9 renders MULTISET on PostgreSQL as correlated
    `jsonb_agg(jsonb_build_array(...))` subqueries (`lookahead-dashboard.sql`). Cold first requests:
    640 / 370 / 410 ms.
11. **Caching layers** (lab 9.5, all reproduced by `scenario-1..5.hurl` against the buggy starter):
    a per-request DataLoader keeps the old Book after a rename in the same mutation request; a Caffeine
    entity cache without eviction; a Redis response-cache key without the viewer; a response cache never
    invalidated by mutations; `Cache-Control: public` on a personal GET, which Varnish then serves to
    another reader. A Spring `DataLoader` parameter must be named after the registered loader ("books"),
    or Spring can't resolve it: `Cannot resolve DataLoader for parameter 'loader'`.
12. A `WebGraphQlInterceptor` can answer from a cache without executing:
    `new WebGraphQlResponse(new DefaultExecutionGraphQlResponse(request.toExecutionInput(), result))`.
13. Through Varnish (`wire.txt`, 50 dashboards): POST 281 request bytes, 0% hits, median 17.3 ms; GET with
    full text 357 bytes, 98%, 0.4 ms; APQ GET 187 bytes, 98%, 0.3 ms. The latency gain is from GET plus
    Cache-Control; APQ's own gain is request size (and skipping parse/validate on the server).
14. **ZGC on a small heap collapses** (`tune-256m/`, 150 dashboards/s, `-Xmx256m`): G1 p50/p95/p99
    10/19/28 ms with 0 failures; ZGC 20,001/60,000/60,001 ms with 2,984 failures, 4,919 `Allocation
    Stall` lines and `OutOfMemoryError: Java heap space` that killed Tomcat's acceptor thread. With JFR
    (settings=profile) and async-profiler attached in all runs.
15. **At `-Xmx1g` every config holds** (`tune-1g.txt`): platform-g1 10/15/27, virtual-g1 9/13/17,
    platform-zgc 12/16/21, virtual-zgc 11/16/21; GC pause sums G1 466/395 ms, ZGC 3/4 ms. ZGC removes the
    pauses but costs throughput (higher p50); virtual threads helped G1's tail here. One run each: noise
    of a few ms is likely, which is lecture 9.8's point about repeating runs.
16. JMH (`jmh.txt`): executing the dashboard document without data takes 22.6 us; with a cached
    PreparsedDocumentEntry 3.1 us, so parse + validate is about 19.5 us per request.
17. Gatling 3.16.1 (`gatling.txt`, `before-after.txt`): cold JVM p50/p95/p99 15/422/837 ms; after a 10 s
    warm-up 14/17/18 ms (batched) and 51/60/64 ms (naive) at 20 dashboards/s. Micrometer's in-app
    `graphql.request.percentile` (client-side histogram, sliding window) reported p99 904 / 520 ms for the
    same runs: the window still held the warm-up (`percentile.txt`). Gatling 3.16 writes `js/stats.js`, not
    `stats.json`; the console summary is the stable thing to parse.
18. async-profiler 4.6's Maven Central jar carries `linux-x64/libasyncProfiler.so` (and arm64, macOS);
    `event=itimer` works in the course container without perf_events. The flame graph and `jfr view
    hot-methods` both show graphql-java's PerLevelDataLoaderDispatchStrategy and the per-key row filter
    in BatchController.
19. java-dataloader 6.0.0 does not remember failed keys: a second `load` of a key whose batch failed is
    sent again (`failcache.txt`). Per-key failures with `newDataLoaderWithTry` fail only that key.
