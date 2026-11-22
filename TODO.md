# TODO.md — remaining work

## Now: user review of Module 18

The user said "continue..." (2026-11-23) after Module 17's report; Module 18 is built. Wait for review before
the capstone.

Decisions for the user:

- [ ] Viaduct (lab 18.4): its starter (`viaduct-dev/cli-starter`, `./gradlew -q run --args="'{ greeting }'"`) was
      not run: running its Gradle wrapper (in the course's Gradle image) was blocked in the authoring session, as
      graphql-java's was for 17.7. Run it (or allow it), then record the output and time in lab 18.4's
      reference `GAPS.md` and lecture 18.4.
- [ ] Lab 17.7's local build of graphql-java (`RELEASE_VERSION=26.1-local ./gradlew publishToMavenLocal
      -PpublishToMavenLocal`) was not run: running graphql-java's Gradle wrapper was blocked in the authoring
      session. Run it yourself (or allow it), then confirm the flags and record the test count and time in the
      reference `INTERNALS.md`.

Decisions still open from Module 15:

- [ ] Rover: accept the Elastic License 2.0 for its composition plugin (then labs could use Rover), or keep The
      Guild's MIT composer (current).
- [ ] Apollo Router's anonymous telemetry: tests run with its default; set `APOLLO_TELEMETRY_DISABLED=true` in
      `contract/fed.sh` and `compose.yaml` if you prefer.

## Verification debt

- [ ] Module 16: re-check pins each term: Prometheus, Tempo (does search still lag 20–30 s behind ingest?),
      Grafana, the OpenTelemetry Java agent, Jib; Hive's usage report schema and client header names.
- [ ] Module 16: usage reports were checked against the course collector only; send one to a real Hive target
      once (needs an account and token).
- [ ] Module 16: no Kubernetes run: the manifests (lab 16.4 Deployment, 16.6 HPA) are checked as YAML and the HPA is
      simulated (`HpaSim.java`); apply them to a cluster (kind or k3s) once, with prometheus-adapter.
- [ ] Module 16: timing-based checks green on one 24-core machine: lab 16.4's 0 failed queries (2 s drain vs 1 s
      propagation), lab 16.5's p95 < 250 ms, lab 16.10's top series; the knee (lab 16.6) is per machine by design.
- [ ] Module 16: the ops stack is Linux x86_64 binaries; arm64 hosts need the arm64 archives and sums.
- [ ] Module 16: CRaC is lecture-only; Jib's `jib:buildTar` needs internet (Docker Hub) and was run once.

- [ ] Rerun the full `bin/course check --starters` (the 2026-11-21 run after Module 10 was stopped at m10/01;
      after Modules 11 to 14 only their own module checks ran). It is CPU-heavy: run it when the machine is free.
- [ ] Module 15: `checkpoint-host.sh` and `docker compose up` verified on Linux (Docker Compose 5.8.0) only; check
      Docker Desktop (paths for `COURSE_ROOT`, user ids). The full `bin/course checkpoint 15` flow (with a learner
      workspace) has not been run end to end; the compose run itself was run by hand.
- [ ] Module 15: re-check pins each term: Apollo Router (2.19.1 and newer), Hive Router (0.3.19), federation-jvm
      7.1.1, composition 0.27.2 and federation v2.10+ support, Nadel (fast releases), graphql-kotlin's default link.
- [ ] Module 15: lab 15.5's GitHub Actions workflow is read by the test, not run on GitHub.
- [ ] Module 14: re-record Open Library each term (`contract/openlibrary/data/`; FINDINGS 8) and re-check its API
      shape; lab 14.4's and 14.10's expected values come from the recording.
- [ ] Module 14: lab 14.5 creates one Kafka topic per test run (`m14-reviews-<time>-<n>`); they pile up in the
      course Kafka. Add a cleanup (admin client delete) or a retention setting.
- [ ] Module 14: lab 14.2's expected strings are CLDR output of the course image's JDK 25; a JDK update may change
      them (re-run the solution and diff).
- [ ] Module 14: SOAP (14.4) and backups/topics/exports in erasure (14.6) are lecture-only.
- [ ] Module 13: lab 13.3 mounts the Docker socket; verified on Linux (Docker 29.10) only. Check Docker Desktop
      (macOS, Windows): socket path, group id (`stat -f %g` fallback), and whether Testcontainers finds the host.
- [ ] Module 13: the MockMvc `HttpGraphQlTester` + bearer token `UNAUTHORIZED` (FINDINGS 7) is observed, not
      explained; find the cause or a Spring issue before teaching more than "use a real port".
- [ ] Module 13: GitHub Actions workflow (lab 13.5) is read, not run; its graphql-java class path (4 jars) was
      checked locally. Run it once in a real repository. Hive and GraphOS checks are described, not run.
- [ ] Module 13: re-check graphql-java's SchemaDiff on a non-null argument with a default (FINDINGS 14) and the
      truncated default-value message; game 13.9 round 06 relies on the false positive.
- [ ] Module 13: MockSpec is RFC 0 (strawman, updated 2026-11-21): re-read before each term; mini-project 13.10
      follows its algorithm names. IntelliJ GraphQL plugin support for `graphql.config.yml` is from its docs.
- [ ] Module 12: npm pins (Apollo Client 4.2.12, React 19.2.9, jsdom 27.4.0, Relay 20.1.1) skip lines that were
      days old on 2026-11-22; move to Apollo Client 4.3, React 19.3, Relay 21 next term and re-run lab 12.6's
      measure. Re-check whether relay-compiler still panics on a `.graphqls` schema name.
- [ ] Module 12: re-check for a Maven plugin for Apollo Kotlin 5 (lab 12.7 would leave Gradle), and whether
      Spring's `documentName` starts sending `operationName` and `HttpSyncGraphQlClient` starts wrapping I/O
      errors in `GraphQlTransportException` (labs 12.2, 12.8).
- [ ] Module 12: lab 12.8's 97% and 85% thresholds are statistical (four runs: 99–100% and 65–71%); watch for
      flakes. Lab 12.10's pty check sleeps 8 s for startup; slow machines may need more.
- [ ] Module 12: Apollo Kotlin's Android `SqlNormalizedCacheFactory` signature and the in-memory nature of
      `optimisticUpdates` are from documentation knowledge, not run (no Android SDK).
- [ ] Module 11: Apollo Client's `Accept: multipart/mixed;deferSpec=20220824` is from Apollo's documentation,
      not tested; RFC 1110 is a draft (re-read before each term; its PR links graphql-java issue #4497, not read).
- [ ] Module 11: lab 11.5's load numbers are single runs on one 24-core machine, first runs include the JIT.
- [ ] Module 11: re-check graphql-java for `@stream` and RFC 1110 payloads, and Spring for GraphQL for an
      incremental transport; simplify lab 11.6 if either lands.
- [ ] PayPal "700 ms p99 per round trip" and "3 to 52 products in 2 years": the Medium post blocks
      automated fetches (403). Verify by hand or replace with another source (lecture 1.2, case-studies tab).
- [ ] Re-verify public API answers in m00/03 and the GitHub schema answers in m00/06 each term.
- [ ] With a GITHUB_TOKEN: run m02/09 bonus holes and m02/12's live check; record the CLI query's
      `rateLimit { cost }` in m02/12's reference notes. Confirm the reference notes' claims that GitHub
      REST has no pinned-items endpoint and that contributions are GraphQL-only.
- [ ] Refresh `tools/schemas/github-*.graphql` and the m02/06 public API type counts each term.
- [ ] Open `/voyager.html` in a browser once (game 3c): only the CDN assets and the JS API names were
      checked with curl, not the rendering.
- [ ] Re-check graphql-java-extended-validation for a release built against graphql-java 26 and Hibernate
      Validator 9; lift the HV 8 pin if so.
- [ ] Re-check MinIO image availability; the course uses SeaweedFS 4.54 meanwhile.
- [ ] Game 4b signatures: refresh from the GitHub snapshot and AniList introspection each term.
- [ ] Re-check DGS for a release built on graphql-java 26; lift lab 6.7's 25.0 pin if so.
- [ ] Re-check the Quarkus 4.0 platform BOM (4.0.0.CR1 on 2026-11-21); move lab 6.8 when it is final.
- [ ] Game 6b numbers came from one 24-core machine; JVM build times for 6b were not timed.
- [ ] m05/11 bug B1 (common pool) is only detectable with parallelism > 1; fine on any multi-core machine.
- [ ] Re-run lab 5.4's audit against Spring for GraphQL each release (11/25 on 2.1.0).
- [ ] Check whether a newer graphql-java fixes the subscription-root `@skip` gap and parses operation
      descriptions; update lectures 2.5 and 2.7 and the module README table if so.

- [ ] Game 7b cards: re-check AniList's `UpdateUser` (19 args), `Media` (75 args), `Json` fields and the
      GitHub counts (73 `databaseId`, 253 of 298 inputs with `clientMutationId`, 147 `Repository` fields).
- [ ] graphql-inspector: move off 7.0.2 once a 7.0.x has been stable two weeks; recheck lab 7.6's two
      tool disagreements (optional argument, deprecation) after any upgrade.
- [ ] RFC stages in Module 7 (ObjectIdentification, SemanticNullability, #1163, #1231, #997) each term.
- [ ] Lab 7.1's SDL-only experiment: the reference notes describe a good answer; no measured run.
- [ ] PostgreSQL 19.0 (released 2026-11-19): move the course service off 18.7 next term; re-run lab 8.2's
      plans (the OR-form filter vs row-value seek) on it.
- [ ] Re-check Spring Data JPA's keyset scroll SQL (OR form, 4.2.0) and Spring for GraphQL's cursor
      strategy bean condition and status codes (400/200) each release; update lectures 8.2 and 8.5.
- [ ] RFC stages in Module 8 (#1163 RFC 1, #1183 RFC 3, #1184 RFC 2, #1073 RFC 3, #1200 RFC 0) each term.
- [ ] Module 9 timing checks (game 9a's p95 at half the baseline, lab 9.6's 90% hit rate, lab 9.7's JMH 2x,
      lab 9.8's winner re-run) were green on one machine; watch them on slow or busy machines.
- [ ] Lab 9.8's reference report has one run per configuration; repeat 5x and report medians.
- [ ] Re-check each release: Spring for GraphQL enabling DataLoader chaining (2.1.0: never), sending
      Cache-Control on GET (2.1.0: none), the GET-mutation `Allow` header; graphql-java's APQ hash check on hits.
- [ ] Keycloak: move off 26.8.2 each term; re-check that a realm import with an `rsa` key provider still
      creates no other keys and that `kid` is still the SHA-256 of the DER public key.
- [ ] Re-check each release: Spring Boot passing `spring.graphql.cors` to the GraphQL WebSocket handler
      (lab 10.7's origin rule depends on it), `AuthenticationWebSocketInterceptor`'s 4401 close, graphql-java
      never suggesting field names and its good-faith introspection default, the 15,000-token parser limit.
- [ ] graphql-cop: 1.15 pins requests 2.25.1; re-check for a release, and that its 13 checks still decide as
      `tests/cop.hurl` re-implements them. Try Clairvoyance releases against graphql-java's messages.
- [ ] GitHub's and Shopify's rate-limit models in lecture 10.6 are from their documentation, not measured.
- [ ] Lab 10.6's Spender (15 s ceiling) and lab 10.4's 1 s timeout (2.5 s ceiling) are time-based; green on one machine.
- [ ] Module 7's CI workflow pins actions/checkout@v4 and setup-java@v4; current majors are v7 and v6.
- [ ] Mini-project 8 and game 8a time-based checks (bot every 20/30 ms) were green on one machine;
      watch for flakiness on slow machines.

- [ ] Module 17: re-check each graphql-java release against `harness/Known.java` (lone CR, U+2028/U+2029, block
      strings with CR or short blank lines, descriptions on operations, empty `{}` in SDL, the fieldless-type
      lookahead, missing locations on result-coercion and null-for-non-null errors); drop rules it fixes.
- [ ] Module 17: re-run graphql-http against lab 17.8 on each release; the four edition conflicts (572B, FDE2,
      7B9B, 86EE) and the 18 MAY-400 notices may be updated to the September 2026 edition.
- [ ] Module 17: the spec baseline is the September 2025 edition; when a new edition ships, re-check the hunt's 15
      answers, lab 17.1's sections and the draft-only features (RFC #1227 empty selection sets).
- [ ] Jazzer 0.30.0 needs the JUnit BOM import and `jazzer.instrument`; re-check on a new release. The fuzz test
      uses about 2 GB.

- [ ] Module 18: no assistant was connected to the MCP servers (tested with `contract/mcp.sh` only); connect one
      (Claude Code's `claude mcp add --transport http ...` is from its documentation) and record a transcript in
      lab 18.2's reference `CONNECT.md`. Lab 18.1's sample LLM run is the authoring model's, after seeing the
      data: replace it with a fair run (schema only, then schema and operations) when an LLM is available.
- [ ] Module 18: re-check each term: Spring AI (2.0.2 pinned; 2.1 for Boot 4.2), the MCP versions the SDK
      negotiates (2025-11-25; spec current 2026-07-28), whether `@apollo/subgraph` 2.15+ works with a newer
      `@pothos/plugin-federation` (then lift the 2.14.4 pin), PostGraphile 5.x (behaviours, Ruru at `/`, its
      graphql-js and Operation Type Existence), the seven RFC stages in `labs/05-spec-frontier/tests/stages.key.json`
      (regenerate from `solution/stages.tsv`) and lecture 18.5's table.
- [ ] Module 18: a Go (gqlgen) reference port for lab 18.6 and a `bin/course go` (or a Go toolchain in the image);
      until then the Go path is unverified.
- [ ] Module 18: the MCP SDK's 404 for an unknown session carries a serialized exception with a stack trace;
      re-check on SDK updates (lecture 18.2, lab 18.3's REDTEAM notes).
- [ ] Module 18: Hasura, pg_graphql, Neo4j GraphQL Library and Dgraph are lecture-only (licences and versions
      checked on GitHub; behaviour from documentation). Apollo MCP Server's design is from its documentation.

## Next modules (in order)

- [ ] Capstone, per plan/syllabus.md

## Infra backlog

- [ ] Testcontainers needs a Docker socket in the course container (PostgreSQL itself is a course service since Module 8)
- [ ] A per-module Shelfie checkpoint repo layout once learners build their own server (Module 5)
