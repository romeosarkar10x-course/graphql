# TODO.md — remaining work

## Status

The course is complete (2026-11-24): Modules 0 to 18 and the capstone are built, every open check that could be
run here was run (`_workbench/2026-11-24-finish/FINDINGS.md`), and `bin/course check --starters` covers every
module. What is left needs something only you have, or is maintenance for each new term.

Supported host: Linux (the user's machines run Arch Linux); Docker Desktop, macOS and Windows are out of scope.

## Needs you

- [ ] **A GitHub token** (`export GITHUB_TOKEN=...` before `bin/course`): run m02/09's bonus holes and m02/12's
      live check, record the CLI query's `rateLimit { cost }` in m02/12's reference notes, and confirm the notes'
      claims that GitHub REST has no pinned-items endpoint and that contributions are GraphQL-only.
- [ ] **A Hive account and token**: send one of Module 16's usage reports to a real Hive target (checked against the
      course's collector only).
- [ ] **PayPal's "3 to 52 products in 2 years"** (`plan/company-case-studies.md`): Medium refuses automated fetches;
      read "Scaling GraphQL at PayPal" in a browser and confirm or drop it. The 700 ms figure is verified.
- [ ] **By eye, if you want** (skipped on purpose, 2026-11-24): Voyager at `/voyager.html` (game 3c) and the
      capstone's web page (`COURSE_PUBLISH=4005`, `client ui`); both were checked with curl only.

## Each term (maintenance)

Pins and outside facts drift. Re-check, re-run the solution, and update the lecture or notes when one moves.

- **Pins** (VERSIONS.md): Spring Boot, Spring for GraphQL, graphql-java; PostgreSQL (19.0 came out 2026-11-19:
  move off 18.7 and re-run lab 8.2's plans); Keycloak (26.8.2; a realm import with an `rsa` provider creates no
  other keys, `kid` is the SHA-256 of the DER key); Prometheus, Tempo (does search still lag 20–30 s?), Grafana,
  the OpenTelemetry agent, Jib; Apollo Router (2.19.1+), Hive Router (0.3.19+), Rover (1.1.0+), federation-jvm
  (7.1.1), composition (0.27.2, federation v2.10+), Nadel, graphql-kotlin; graphql-inspector (off 7.0.2 after a
  stable 7.0.x; recheck lab 7.6's two disagreements); Spring AI (2.0.2; 2.1 for Boot 4.2) and the MCP versions it
  negotiates; PostGraphile 5.x; `@apollo/subgraph` 2.15+ with a newer `@pothos/plugin-federation` (lift the 2.14.4
  pin); npm pins of Module 12 (Apollo Client 4.3, React 19.3, Relay 21; re-run lab 12.6's measure; does
  relay-compiler still panic on a `.graphqls` schema name?); Go and gqlgen (lab 18.6's Go port); Jazzer (0.30.0
  needs the JUnit BOM import and `jazzer.instrument`); graphql-cop (1.15 pins requests 2.25.1; its 13 checks
  against `tests/cop.hurl`) and Clairvoyance; MinIO (the course uses SeaweedFS 4.54); the GitHub Actions majors
  (checkout v7, setup-java v6, setup-node v7, upload-artifact v7).
- **Lifted pins, when upstream catches up**: graphql-java-extended-validation built for graphql-java 26 and
  Hibernate Validator 9 (lift the HV 8 pin); DGS on graphql-java 26 (lift lab 6.7's 25.0 pin); the Quarkus 4.0
  platform BOM final (4.0.0.CR1 on 2026-11-21; move lab 6.8).
- **graphql-java behaviour the course teaches around**: the subscription-root `@skip` gap and operation
  descriptions (lectures 2.5, 2.7); never suggesting field names, the good-faith introspection default, the
  15,000-token parser limit (Module 10); SchemaDiff on a non-null argument with a default and its truncated
  message (Module 13 FINDINGS 14; game 13.9 round 06 relies on it); `@stream` and RFC 1110 payloads (simplify lab
  11.6 if they land); Module 17's `harness/Known.java` list (lone CR, U+2028/U+2029, block strings, operation
  descriptions, empty `{}` in SDL, missing error locations); the APQ hash check on hits.
- **Spring behaviour**: DataLoader chaining (2.1.0: never), Cache-Control on GET (none), the GET-mutation `Allow`
  header; `spring.graphql.cors` reaching the WebSocket handler (lab 10.7), `AuthenticationWebSocketInterceptor`'s
  4401 close; `documentName` sending `operationName` and `HttpSyncGraphQlClient` wrapping I/O errors (labs 12.2,
  12.8); Spring Data JPA's keyset scroll SQL and the cursor strategy bean condition (lectures 8.2, 8.5); Boot's
  JWT decoder choice (the labs avoid `issuer-uri` for offline tokens); re-run lab 5.4's audit (11/25 on 2.1.0).
- **Outside data and specs**: Open Library recordings (`m14-patterns/contract/openlibrary/data/`; labs 14.4 and
  14.10 expect values from them); public API answers in m00/03 and the GitHub schema answers in m00/06;
  `tools/schemas/github-*.graphql` and m02/06's type counts; game 4b signatures and game 7b's AniList and GitHub
  counts; lab 14.2's CLDR strings after a JDK update; the RFC stages in Modules 7, 8, 11 and 18
  (`labs/05-spec-frontier/tests/stages.key.json`, regenerate from `solution/stages.tsv`); MockSpec (RFC 0) for
  mini-project 13.10; a new spec edition (the hunt's 15 answers, lab 17.1's sections, RFC #1227); graphql-http
  against lab 17.8; Hive's usage report schema and header names; the MCP SDK's 404 body with a stack trace.

## Known limits (by design, documented where they apply)

- Timing-based checks were green on one 24-core machine; watch them on slow or busy machines: game 9a's p95,
  labs 9.6 (90% hit rate), 9.7 (JMH 2x), 10.4 and 10.6 (time ceilings), 11.5 (single load runs), 12.8 (97% and
  85% thresholds, statistical), 12.10 (8 s pty start-up), 16.4, 16.5 and 16.10, mini-project 8 and game 8a (bots
  every 20/30 ms), game 6b's numbers. m05/11's bug B1 needs parallelism above 1.
- Lecture-only by design: SOAP (14.4), backups/topics/exports in erasure (14.6), CRaC (16), Hasura, pg_graphql,
  Neo4j GraphQL and Dgraph (18.4), Apollo MCP Server, GitHub's and Shopify's rate-limit models (10.6), Hive and
  GraphOS schema checks (13.5), the IntelliJ GraphQL plugin's `graphql.config.yml` support (13, from its docs),
  Android (12.7; the API was checked from Apollo Kotlin's source).
- CI workflows in labs 7.6, 13.5, 15.5 and the capstone are read by the tests, not run on GitHub.
- Kubernetes is simulated in the labs (`ReadyBalancer`, `HpaSim`); the manifests were applied once to kind.
- Lab 14.5 prunes its own Kafka topics; other labs' topics are left to the course Kafka's retention.
- The capstone's group variant (one subgraph each) is the same checker on a shared repository; it was not run
  with several people.
