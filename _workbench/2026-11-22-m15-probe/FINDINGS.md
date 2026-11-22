# Module 15 probe findings (2026-11-22)

Everything below was run in this folder or by the labs' tests; files are named per finding. Versions in
`versions/` (Maven Central, npm, GitHub releases, licences from the GitHub API).

1. **Rover needs a licence acceptance.** Rover's composition (supergraph plugin) asks for the Elastic License 2.0
   to be accepted (`APOLLO_ELV2_LICENSE=accept`). Not accepted on the user's behalf: composition uses
   `@theguild/federation-composition` 0.27.0 (MIT) with graphql 16.15.0 (`contract/compose/`, `composer/`). It
   supports federation v2.0 to v2.9 only.
2. **Apollo Router 2.19.0 unlicensed** (ELv2): `@defer`, `--hot-reload` (200 queries during a reload, 0 errors),
   progressive override `percent(50)`, header propagation, plan exposure (`plugins.experimental.expose_query_plan`
   + header `Apollo-Expose-Query-Plan: true`) all work. **Subscriptions are a licence violation: the router refuses
   to start.** Header rules moved to `headers.all.request.operations: [...]`; the old shape logs a warning.
   Subgraph errors are redacted by default. Introspection mixed with concrete fields in one operation is refused
   (`MIXED_INTROSPECTION`, router issue #2789). It has `--anonymous-telemetry-disabled` / `APOLLO_TELEMETRY_DISABLED`
   (strings in the binary); the course leaves telemetry at its default. `ops/apollo-ops.txt`, `lab06-probe.txt`.
3. **Hive Router 0.3.9** (MIT): config only from `ROUTER_CONFIG_FILE_PATH` or `./router.config.yaml`; a `--config`
   flag is silently ignored. `SUPERGRAPH_FILE_PATH`, `PORT`, `HOST` env; a `http.host` in the file wins over `HOST`.
   `/graphql`, `/health`. Plans with `query_planner.allow_expose` + `hive-expose-query-plan: true`. Override labels
   accept `percent(50)` and expressions (`random_float(0.0, 100.0) < 50.0`). Subscriptions start unlicensed;
   **`@defer` is rejected as "Unknown directive"**. Timeout errors say `SUBGRAPH_REQUEST_TIMEOUT` with the service.
   `ops/hive-ops.txt`. Its `--version` starts a server (killed by hand).
4. **Same plans**: Apollo and Hive produced identical step plans for the 5 lab queries, the N+1 query and the 10
   puzzles. `lab05-plans.txt`, `puzzle-plans.txt`. The cross-subgraph N+1 query: one request per subgraph.
5. **Load** (contract `Load.java`, 2000 requests, 16 concurrent, local subgraphs): Apollo p50 5 ms, p95 16, p99 29,
   RSS 290 MB; Hive p50 3, p95 5, p99 12, RSS 151 MB. One laptop, one workload. `lab06-probe.txt`.
6. **Spring for GraphQL 2.1 + federation-jvm 7.0.0** (not in Boot 4.2.0's BOM; 7.1.1 was too new): a `@key` type
   without `@EntityMapping` stops start-up ("Unmapped entity types: 'Review'"). Single and DataLoader forms raise
   `RepresentationNotResolvedException` for null, an `INTERNAL_ERROR` unless mapped (`@GraphQlExceptionHandler`);
   the List form returns null quietly. A Flux loader with a null element fails the whole batch. The SDL printer
   writes `@key(fields : "id", resolvable : true)`. `entity-probe.txt`, `spring/`.
7. **Entity costs** (lab 15.4 variants): single form 6 store queries against 1 (List) for five books; unbatched
   `reviews`/`reviewCount`/`averageRating` 15 against 1. The router sends one `_entities` request per subgraph per
   step. `lab04-variants/counts.txt`.
8. **No key**: removing Book's `@key` gives `INVALID_FIELD_SHARING` on `Book.id`; with a keyed entity interface
   (`Media`) present, `INTERFACE_KEY_NOT_ON_IMPLEMENTATION` comes first. `nokey/`.
9. **Nadel** (probed on 0.44642751879 of 2026-11-20; the labs pin 0.42134418907 of 2026-11-06, re-run by lab 15.2; Apache-2.0) brings its own graphql-java; the old `service X { }` wrapper no
   longer parses. `@hydrated` with a list field and `identifiedBy` batches and de-duplicates (5 reviews, 1 call,
   3 ids); with `book(id:)`, 4 calls for 4 reviews. `@renamed(from:)` removes the old name for clients; the service
   sees an alias. `nadel/run1.txt`, `run2.txt`, `exp/`.
10. **graphql-kotlin 11.0.0** (federation-jvm 7.0.0, graphql-java 26.1; Kotlin 2.4.20, kotlin-maven-plugin, shade
    3.6.2) links federation **v2.13** by default, which the Guild composer rejects. Pinned with `@LinkDirective(url
    = ".../v2.9", as = "", import = [...])` on a class passed as `schemaObject`; it then composes. `kotlin/run.txt`.
11. **Moving `Review.book`** under traffic every 50 ms with blue/green deploys and hot reload: the four-state order
    (claim with `@override`, remove, tidy) gave 638 requests, 0 failed; removing first failed 193 of 478.
    `lab07-wrong-order/`.
12. **Docker Compose 5.8.0 on the host**: `docker compose up --wait` of mini-project 15.8 (build, 4 subgraphs
    healthy, supergraph from `_service`, router healthy) answered the club page; bind mounts need host paths, so the
    checkpoint runs it from `checkpoint-host.sh` outside the course container. `compose-up.txt`.
13. Facts for lectures: InfoQ "Evolving the Federated GraphQL Platform at Netflix" (Kavitha Srinivasan,
    2023-08-14): "more than 200 services" in the federated graph (`docs/infoq.txt`). Composite Schemas roadmap:
    "Proposal Stage" (`docs/composite-roadmap.md`). feddi: JVM-native, by Andi Marek, commercial (`docs/feddi.txt`;
    not run). DGS: `@DgsEntityFetcher(name = "Show")` (`docs/dgs-federation.txt`). Licences and languages:
    `versions/licences.txt`.
