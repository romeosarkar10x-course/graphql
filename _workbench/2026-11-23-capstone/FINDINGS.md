# Capstone build findings (2026-11-23)

Probe scripts and their output are in `probe/` (`lib.sh` starts the reference jars inside the course image).

## Hive Router 0.3.9 (the reference's router)

1. **Config paths**: paths inside `router.yaml` (persisted documents, JWKS) are relative to the config file;
   `SUPERGRAPH_FILE_PATH=router/supergraph.graphql` (relative to the working directory) failed with "Failed to
   resolve path ... for key `supergraph`", an absolute path works (`scripts/router.sh`). Config keys taken from
   the source at tag `hive-router/v0.3.9` (`bin/router/src/config/*.rs`, `bench/` and `e2e/` configs): the
   repository has no published `router-config.schema.json` at that tag (404).
2. **Trusted documents**: `persisted_documents` with a file `{"<id>": "<document>"}`, `require_id: true`. A raw
   query is `PERSISTED_DOCUMENT_ID_REQUIRED`, an unknown id `PERSISTED_DOCUMENT_NOT_FOUND`, both HTTP 200 with
   no `data` (`router1.txt`). Default selectors: `documentId` and `extensions.persistedQuery.sha256Hash`.
3. **Demand control** from `@listSize`/`@cost` (federation v2.9, composed by The Guild's composer 0.27.0):
   `x-cost-estimated` 24 (ToolPage, 3 filtered), 77 (12), 258 (50), MyLoans 35 to 53, OverdueReport 145 at its
   defaults and 7,755 to 7,782 at 50 x 50 (the figure moved by a few dozen between supergraph versions). Over the
   max: `COST_ESTIMATED_TOO_EXPENSIVE`, HTTP 200. Without `default_list_size` it warns that unannotated lists
   count as 0. A page of 10,000 is refused by cost before the resolver's own limit (attack A09).
4. **JWT with `require_authentication: false`**: an invalid token (expired, forged, `none`, wrong audience) is
   logged (`jwt token error`) and the request continues as anonymous, with `Authorization` still propagated, so
   the subgraph answers 401. With Spring's default empty 401 body the client saw `SUBGRAPH_RESPONSE_BODY_EMPTY`;
   with a JSON GraphQL error body it sees the classification, but the message was "Unexpected error" until
   `error_masking.all.enabled: false`.
5. **No batching**: a JSON array of 50 requests is "Failed to parse GraphQL request payload".
6. **Request deduplication is on by default** (`traffic_shaping` dedupe): the same load mix runs 4.81 SQL
   statements per request at concurrency 1 and 2.47 at concurrency 8 (`stmts.txt`). 60 concurrent `MyLoans`
   with two members' tokens interleaved each got their own answer (attack A17), so the dedupe key includes the
   propagated header.
7. **Tracing**: the router continues an incoming W3C `traceparent`; its spans reach Tempo about 3 s after the
   subgraphs' (`ops3.txt`). Without Tempo it logs `BatchSpanProcessor.Flush.ExportError` and carries on.
8. **Subscriptions**: `subscriptions.enabled` + a client POST with `Accept: text/event-stream` and a
   `documentId` streams `event: next` lines; the router reaches the Spring subgraph over HTTP streaming
   (`sub1.txt`).
9. **Query plan** (with `query_planner.allow_expose` and `hive-expose-query-plan: true`): the catalogue page is
   one inventory fetch and one batched lending `_entities` fetch (`claims.txt`).

## Spring

10. **`issuer-uri` with `public-key-location` (Boot 4.2.0)**: the JWT decoder fetched Keycloak's discovery
    document on the first token and failed with `UnknownHostException: keycloak` when Keycloak was not running
    (both `spring-boot-starter-oauth2-resource-server` and `spring-boot-starter-security-oauth2-resource-server`).
    Module 10's FINDINGS 4 says the same configuration made no request to Keycloak; that probe probably ran with
    Keycloak up. Not re-checked in Module 10 (TODO). The reference builds its `NimbusJwtDecoder` from the key
    with issuer and audience validators instead.
11. Prometheus scrapes become traces unless filtered: an `ObservationPredicate` drops `/actuator` server
    observations; Spring Security's filter-chain observations are separate (`management.observations.enable.spring.security: false`).
12. `INSERT ... ON CONFLICT DO NOTHING` consumes a sequence value even when nothing is inserted (loan ids
    `l100`, then `l102`).
13. A WebGraphQlInterceptor that resolves the caller once per request cut lending's statements for a page of 20
    entities from 27 to 4 (a member lookup ran per field before).

## The reference's design, changed by probes and tests

14. **`Tool.available: Boolean!` across the subgraph boundary**: with lending stopped, the router's error
    (`affectedPath: tools.edges.@.node`) propagated to `data: null` for the whole catalogue. Made nullable; the
    catalogue then loads with `available: null` and one error (`claims.txt`).
15. **Cursor sort order**: `InventoryGraphQlTest` caught a NEWEST cursor accepted under NAME_ASC (both had two
    parts); cursors now carry their sort order.
16. Tests: 19 (8 unit, 4 inventory GraphQlTester, 5 lending WebGraphQlTester, 2 schema-diff); PIT 19 of 19
    mutants killed in `LendingRules`; removing `Tool.barcode` from the contract fails the schema-diff gate.
17. Load (`attacks-load.txt`): 54,222 requests in 20 s at concurrency 8, 2,711/s, p95 4.4 to 5.3 ms, 0 errors,
    one 24-core machine; 344/s at concurrency 1.
18. Attacks: 18, all blocked (`attacks2.txt`).

## The checker

19. Capcheck: graphql-java 26.1 always defines `@defer`, so "the schema has @defer" means nothing; the checker
    looks for `@defer` in operations instead.
20. Module 7's design lint (`references`) flags `ToolNotFound.toolId`; the reference argues it in its design doc.
21. `find "$LAB_DIR"` does not descend into a symlinked solution directory; `find "$LAB_DIR/"` does.
22. A single, non-federated service is checked by introspection instead of `_service` (tried with Module 10's
    lab 10.2 solution: 38 coordinates matched).
23. GitHub Actions majors on 2026-11-23 (GitHub API): checkout v7.0.1, setup-java v6.3.0, setup-node v7.1.0,
    upload-artifact v7.0.2.
