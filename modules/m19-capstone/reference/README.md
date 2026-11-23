# Toolshed

The capstone's reference product: a community tool library's catalogue and lending desk, as a federated
GraphQL API in Java. It is not one of the capstone briefs, on purpose: it shows what "done" looks like for every
requirement without being an answer to copy. Graders read it; learners should not need to.

## Layout

| Path | What |
| --- | --- |
| `capstone.json` | The manifest the course checker reads (`../contract/MANIFEST.md`) |
| `contract/` | The two subgraph schemas (the source of truth) and the released copies for the schema-diff gate |
| `operations/` | Every client operation; `scripts/persist.sh` turns them into the router's trusted documents |
| `inventory/`, `lending/`, `common/` | The subgraphs (Spring Boot 4.2, Spring for GraphQL, federation-jvm 7.0.0) and what they share |
| `client/` | The Java client (Spring `HttpSyncGraphQlClient`) and the web UI's server; `ui/index.html` is the page |
| `router/` | Hive Router 0.3.9's config, the composed supergraph, the JWKS and the persisted documents |
| `tests/api.hurl` | The conformance suite: 24 requests through the router |
| `ops/dashboard.json` | The Grafana dashboard |
| `scripts/` | `run.sh`, `router.sh`, `compose.sh`, `persist.sh`, `attacks.sh`, `Load.java`, `Jwks.java` |
| `docs/` | Pitch, design, ADRs, the stage 1 review, attack report, load test, runbook, demo script |
| `.github/workflows/ci.yml` | CI: contract freshness, `mvn verify`, PIT |

## Build, run, test

Inside the course image (`bin/course shell`), from this directory, with PostgreSQL up
(`docker compose -f docker/compose.yaml up -d postgres` from the course root, or `bin/course ref up`):

```bash
mvn -q -DskipTests package          # all jars
scripts/run.sh                      # inventory :4001, lending :4002, router :4000 (Ctrl-C stops them)
# in a second shell (bin/course shell):
hurl --test --variables-file tests/ids.env --variable api=http://localhost:4000/graphql \
  --variable token_member=$(bin/course token ada --aud toolshed-api) \
  --variable token_other=$(bin/course token olu --aud toolshed-api) \
  --variable token_admin=$(bin/course token root --aud toolshed-api) \
  --variable token_expired=$(bin/course token ada --aud toolshed-api --expired) --variable run=$RANDOM tests/api.hurl
scripts/attacks.sh                  # 18 attacks, all should say BLOCKED
java scripts/Load.java --token $(bin/course token ada --aud toolshed-api)
```

(Inside the image `bin/course token` is `java /course/modules/m10-security/contract/Token.java`.)

Tests: `COURSE_DOCKER=1 bin/course shell`, then `mvn verify` (19 tests; the Testcontainers ones are skipped
without Docker) and `mvn -pl lending -am verify -Ppit -Dtest='LendingRulesTest,SchemaCompatibilityTest'
-Dsurefire.failIfNoSpecifiedTests=false` (PIT: 19 of 19 mutants killed).

The course checks it as every capstone is checked: `bin/course test m19/01 --solution` through `m19/04`.

## Where each requirement is met

| Requirement | Where |
| --- | --- |
| Design doc, diagram, nullability, errors, auth, ADRs | `docs/DESIGN.md`, `docs/adr/` (7) |
| Contract first, operations, Hurl suite | `contract/`, `operations/` (10 operations, 1 fragment), `tests/api.hurl`; the subgraphs load the schema from `contract/` |
| 15+ types, interface, union with sealed types, `@oneOf` | 34 types; `ToolSpecs`, `Problem`; `SearchResult`, `BorrowResult`, `ReturnResult`; `ToolLookup` |
| Cursor connections with filter and sort | `tools(first, after, filter, sort)`, keyset SQL, cursors that name their sort |
| Typed errors, idempotency | `BorrowResult`, `ReturnResult`; `idempotencyKey` (ADR 0005) |
| Real time | `availabilityChanged`, over SSE through the router |
| DataLoader, N+1 detector | `@BatchMapping`, DataLoaders, `InventoryGraphQlTest.statementsDoNotGrowWithThePage` |
| Spring Security, field-level authorization, cost limits, trusted documents | `lending` SecurityConfig and LendingController; `router/router.yaml` |
| Java client and a UI | `client/` (CLI), `ui/index.html` (served by `client ui`) |
| CI with unit, GraphQlTester, Testcontainers, schema-diff and PIT | `.github/workflows/ci.yml`, the tests in each module |
| 2 subgraphs behind a router | inventory, lending, Hive Router |
| Tracing, a Grafana dashboard, a runbook | OTLP from all three to Tempo; `ops/dashboard.json`; `docs/RUNBOOK.md` |
| Demo | `docs/DEMO.md` |
