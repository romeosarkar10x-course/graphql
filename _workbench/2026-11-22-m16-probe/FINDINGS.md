# Module 16 probe findings (2026-11-22)

Scripts `probeN.sh` in this folder, output in `probeN*.txt`. Versions and release dates in `versions/`.

1. **Pins** (released at least two weeks before 2026-11-22): Prometheus 3.16.0 (2026-11-05), Tempo 3.1.1
   (2026-10-21), Grafana 13.3.0 (2026-10-27; 13.3.1 of 2026-11-10 too new), OpenTelemetry Java agent 2.33.1
   (2026-10-26; 2.34.0 too new), Resilience4j 2.4.0, Jib Maven plugin 3.5.2 (2026-07-14). Archives checked against
   the projects' published SHA-256 sums (`versions/sums-*.txt`, Tempo's `SHA256SUMS`). Grafana's tarball is 482 MB
   (1.4 GB unpacked).
2. **Tempo 3.1.1 single binary** needs `live_store.shutdown_marker_dir`, `live_store.wal.path`,
   `block_builder.wal.path` and `backend_scheduler.local_work_path`: all default to `/var/tempo` and it fails to
   start without write access there (`probe` first run). A trace is readable by id within a second of export; a
   TraceQL search finds it only 20 to 30 s later (`probe2.txt`), whatever `max_trace_idle`, `max_trace_live`,
   `flush_check_period` or `max_block_duration` were (tried, no change; removed again). Tests read traces by id
   with their own `traceparent`.
3. **Boot 4.2 + Spring for GraphQL 2.1 defaults** (`probe1.txt`, `probe3.txt`): spans `http post /graphql` >
   `graphql query` (attributes `graphql.operation.name`, `graphql.operation.type`) > `graphql field <name>` for
   non-trivial data fetchers only (attributes `graphql.field.name`, `graphql.field.path`; no parent type) and
   `graphql dataloader arraylist` (`graphql.loader.name=Book.reviews`) for batch mappings. Metrics
   `graphql_request_seconds{graphql_operation_type, graphql_outcome}` (no operation name),
   `graphql_datafetcher_seconds{graphql_field_name, graphql_error_type, graphql_outcome}`,
   `graphql_dataloader_seconds`. Spring's `graphql.operation.name` is a span attribute, not a metric tag.
4. Custom `DefaultExecutionRequestObservationConvention` and `DefaultDataFetcherObservationConvention` beans are
   picked up by Boot (`probe4.txt`): span `graphql field Book.similar`, tags `graphql_field_coordinate`,
   `graphql_operation_name`, `graphql_client_name`. The operation name comes from the request's `operationName`;
   a request that only names the operation inside the document shows as `anonymous`.
5. Boot 4 auto-configures `RestClient.Builder` only with `spring-boot-starter-restclient`; without it the app does
   not start ("required a bean of type RestClient$Builder"). With it, calls to the reviews service are `http get`
   client spans. `spring-boot-starter-opentelemetry` also brings `micrometer-registry-otlp`, which pushes metrics
   to localhost:4318 unless `management.otlp.metrics.export.enabled=false`. The span exporter's default batch delay
   made traces appear 5 s late; `management.opentelemetry.tracing.export.schedule-delay=500ms` in tests.
6. **Hive usage reports** (`docs/`): report v2 JSON schema from graphql-hive/console
   (`packages/services/usage/usage-report-v2.schema.json`), headers `X-Usage-API-Version: 2` and
   `Authorization: Bearer <token>` (integration test), default endpoint `https://app.graphql-hive.com/usage`, client
   headers `x-graphql-client-name` / `graphql-client-name` (+ `-version`) (`docs/usage.ts`). Hive's `fields` list
   types, field coordinates, argument coordinates and a `!` variant (`docs/hive-collect-schema-coordinates.spec.ts`).
   Not sent to Hive (no account): the course collector checks the same shape. Apollo Router's own client headers are
   `apollographql-client-name` / `-version` (strings in the 2.19.0 binary).
7. **Seven simulated days** (`probe7.txt`, Traffic seed 16): 1,460 operations, 23 reports, 0 rejected. Field
   coordinates unused in the last 7 days: `Book.isbn10` (only iOS 2.7.0, 8 to 17 days ago), `Review.book`,
   `User.id`, `Subscription.reviewAdded` (subscriptions are not in the replay: usage of subscriptions needs
   `subscriptionOperations`). `Club.memberCount` (deprecated) is still used by iOS 2.8.0 (22 operations).
8. **Rolling deploy, 200 subscriptions** (`probe5-*.txt`, `probe6.txt`; writer every 50 ms, ~490 messages,
   both replicas restarted in turn behind `ReadyBalancer`, 1 s propagation, no retry): no resume: 1,318 and 1,429
   messages dropped; resume (`after`) without the drain: 0 dropped, 6 failed queries of 4,103; resume + drain
   (1 s) : 0 dropped, 1 failed of 4,164; resume + 2 s drain + `Connection: close` while draining: 0 dropped, 0
   duplicates, 0 failed of 4,166. A replica's stop took 9 to 11 s.
9. **Reliability** (`probe8.txt`, open model 100 club pages/s for 12 s; reviews 2 s slow at 4 s, killed at 8 s):
   no timeouts/breaker: p95 2,002 ms, p99 2,006, per-second p95 2,003 to 2,034 during the slow phase; bulkhead 16 +
   breaker (20 calls, 50% failures or slow > 300 ms, 2 s open) + 500 ms read timeout: p95 3 ms, p99 502, max 505,
   0 failed pages, 839 degraded (REVIEWS_UNAVAILABLE). A closed-loop load (16 workers) hid the slow phase: p95 4 ms
   because the workers were stuck (first try, kept in `probe8.txt` history); `Peak.java` is open-model.
10. **Cost** (`probe7.txt`): `{ books(first: 2) { title similar { title } } }` = 11; the club query with members,
    readingList and reviews = 38 (the rule in `CostMetric.java`).
11. **Game day** (`probe9.txt`, seed 3, 75 s): from PromQL alone the slow field (`Query.search` 247 ms average
    vs `Book.similar` 30), the leaking client (`shelfie_subscriptions_open{web}` 30, others 0) and the abusive
    client (android 131,256 cost/s vs < 200) stand out. Answer keys for seeds 3, 7, 16, 42 match `GameDay.answers`.
12. Python in the image has no YAML module; Jackson 3's `jackson-dataformat-yaml` 3.1.8 (with snakeyaml-engine
    3.0.1) joined the course library for HPA manifests.
13. **Jib 3.5.2** (`jib.txt`): `mvn package jib:buildTar` inside the course image (no Docker daemon; fetches `eclipse-temurin:25-jre` from Docker Hub) wrote a 159 MB `target/jib-image.tar`; `docker load` + `docker run` on the host: user 1000:1000, port 4000, entrypoint `java -XX:MaxRAMPercentage=75 -cp @/app/jib-classpath-file dev.shelfie.ops.ShelfieOps`, `/actuator/health/liveness` UP. Image and tar deleted afterwards.
14. **OpenTelemetry Java agent 2.33.1** on the lab 16.1 starter (`probe10*.txt`): 3 spans by default: `POST /graphql` (tomcat-10.0), `query` (graphql-java-20.0 instrumentation; `graphql.operation.name`, `graphql.document` with arguments replaced by `?`) and `GET` to the reviews service (http-url-connection). With `-Dotel.instrumentation.graphql.data-fetcher.enabled=true`: 13 spans, one per data fetcher named by the field name only (`similar`, `reviews`), path in `graphql.field.path`, including the batched `reviews` field (its time is the wait for the batch).
