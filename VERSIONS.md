# Pinned versions

Verified against Maven Central and GitHub releases on **2026-11-21**
(raw probe output: `_workbench/2026-11-21-version-probe/`). Re-check each term.

| Component | Pinned | Plan said | Note |
|---|---|---|---|
| JDK | 25 (Temurin 25.0.5, Ubuntu 24.04 base) | 25 LTS | 24.04 base because Hurl 8.1.0 links `libxml2.so.2`, renamed in 26.04 |
| Maven | 3.9.16 | Gradle (changed, CONTEXT.md §2.5) | no Maven 4 GA on Central yet |
| Spring Boot | 4.2.0 | 4 | |
| Spring for GraphQL | 2.1.0 (from Boot BOM) | 2.0 | 2.0 features (JSpecify inspection, `DgsGraphQlClient` multi-query) still apply |
| graphql-java | 26.1 (from Boot BOM; 26.2 exists) | 26 | |
| Jackson | 3.1.8 (`tools.jackson`) | — | Boot 4 is on Jackson 3; annotations stay `com.fasterxml.jackson.core` 2.21 |
| Netflix DGS | 12.1.1 | 11 | needs graphql-java **25.0**: on 26.1 startup fails `NoClassDefFoundError: graphql/schema/idl/DirectiveInfo`; lab 6.7 pins `graphql-java.version` 25.0 |
| DGS codegen (Maven) | `io.github.deweyjose:graphqlcodegen-maven-plugin` 3.11.0 | DGS Gradle plugin | official DGS codegen is Gradle-only |
| java-dataloader | 6.0.0 | — | |
| extended-scalars / extended-validation | 24.0 / 24.0 | — | built for graphql-java 24.1; both work on 26.1 (probes in `_workbench/2026-11-21-m03-probe/`) |
| Hibernate Validator / Expressly | 8.0.6.Final / 5.0.0 | — | extended-validation 24.0 breaks on HV 9.2.0 (`NoSuchMethodError` in `MessageInterpolatorContext`) |
| H2 | 2.5.252 | — | lab 4.4 |
| AWS SDK v2 `s3` (presigner only) | 2.59.5 | — | reference server, lab 4.6; HTTP clients excluded |
| SeaweedFS | 4.54 (`chrislusf/seaweedfs`) | MinIO | MinIO images are gone from Docker Hub and quay.io (pull denied, 2026-11-21) |
| GraphQL Voyager | 2.1.0 (jsDelivr) | — | `/voyager.html`, game 3c |
| jspecify | 1.0.0 | — | mini-project 3 |
| Testcontainers | 2.0.5 | — | |
| PostgreSQL | 18.7 (`postgres:18.7-alpine`, course service) | — | Module 8 on. 19.0 was released 2026-11-19, two days before; move to it next term. Collation `en_US.utf8` (libc) |
| Redis | 8.8.4 (`redis:8.8.4-alpine`, course service) | — | Module 9.5's response cache, via spring-boot-starter-data-redis (Lettuce 7.8.0, Boot BOM) |
| Varnish | 9.1.0 (`varnish:9.1.0`, course service) | — | Module 9.6; started with `-S none`, admin port inside the course network only; `tools/varnish.py` loads the backend. No `if()` expression in its VCL |
| datasource-proxy | 1.11.1 | — | Module 9 statement counter |
| java-dataloader | 6.0.0 (from graphql-java 26.1) | — | chaining is off by default and Spring never enables it (Module 9.3) |
| Caffeine | 3.3.1 (Boot BOM) | — | Module 9.5, 9.6, 9.8 |
| jOOQ | 3.21.9 (Boot BOM; 3.21.10 on Central) | — | Module 9.4 MULTISET; spring-boot-starter-jooq, no code generation |
| JMH | 1.37 | — | Module 9.7 |
| Gatling | 3.16.1, gatling-maven-plugin 4.22.3 | k6 | Module 9.7, Java DSL; Maven, no Node in the image |
| async-profiler | 4.6 (Maven Central jar, which carries the native libraries) | — | `event=itimer` in containers |
| Keycloak | 26.8.2 (`quay.io/keycloak/keycloak:26.8.2`, course service, dev mode) | Testcontainers | Module 10; realm import with the course signing key (`modules/m10-security/contract/keys/`); `kid` = base64url(SHA-256(DER public key)) |
| Spring Security | 7.2.0 (Boot BOM) | 7 | Module 10; `resource_metadata` (RFC 9728) in the Bearer challenge |
| Bucket4j | 8.22.0 (`com.bucket4j:bucket4j_jdk17-core`, 2026-11-03) | — | Module 10.4 and 10.6 cost budgets; refuses to consume 0 tokens |
| graphql-cop | 1.15 (2025-11-07, git tag) | — | lab 10.8, `bin/course seckit`, own image `docker/seckit.Dockerfile` (python 3.13-slim); its pinned requirements (requests 2.25.1) installed as is |
| Clairvoyance | 2.5.5 (PyPI) | — | lab 10.8; recovers nothing from graphql-java 26.1 (no suggestions, unparsed messages) |
| Apache Kafka | 4.4.0 (`apache/kafka:4.4.0`, 2026-10-22, course service, one KRaft node, 384 MB heap) | — | Module 11.4; client 4.4.0 and Spring for Apache Kafka 4.2.0 from the Boot BOM (`spring-boot-starter-kafka`) |
| Debezium | 3.7.1.Final (`debezium-embedded`, `debezium-connector-postgres`, 2026-11-06) | — | Module 11.5, embedded engine; works with the Boot BOM's Kafka 4.4.0 libraries; PostgreSQL runs with `wal_level=logical` |
| Reactor | 2026.0.0 BOM (Boot) | — | Module 11 sinks and operators |
| graphql-java incremental delivery | 26.1, `ExperimentalApi.ENABLE_INCREMENTAL_SUPPORT` | `@defer` and `@stream` | `@defer` only (`@stream` is an unknown directive); payloads in the earlier draft's shape (`path`/`label`, no `pending`/`completed`); Spring for GraphQL 2.1 has no incremental transport |
| DGS codegen client API | `generateClientApi` in graphqlcodegen-maven-plugin 3.11.0, `graphql-dgs-codegen-shared-core` 8.8.0 (2026-11-02) | DGS Gradle plugin | Module 12.3 with Spring's `DgsGraphQlClient`; no DGS framework, so no graphql-java 25 pin. Generated requests inline arguments as literals |
| SmallRye GraphQL client | `quarkus-smallrye-graphql-client` (Quarkus platform 3.40.1.2) | — | Module 12.4; `@Id String` is sent as nullable `ID` without `@NonNull` |
| Resilience4j | 2.4.0 (`resilience4j-retry`, `resilience4j-circuitbreaker`, 2026-03-14) | — | Module 12.8; `HttpSyncGraphQlClient` surfaces RestClient's `ResourceAccessException`, so retry that, not `GraphQlTransportException` |
| JLine | 4.7.0 (`org.jline:jline`, 2026-11-12) | — | mini-project 12.10; needs `Enable-Native-Access: ALL-UNNAMED` in the manifest on Java 25 |
| Node.js (web lab) | 24.22.1 (`node:24.22.1-slim`, official image, `bin/course web`) | — | lab 12.6 only; the course image stays Java-only |
| Apollo Client / React / jsdom | 4.2.12 / 19.2.9 / 27.4.0 | Apollo Client, urql, Relay | lab 12.6; 4.3.8, React 19.3.1 and jsdom 31 were days old on 2026-11-22, the previous lines are pinned; urql is lecture-only |
| Relay | 20.1.1 (`react-relay`, `relay-runtime`, `relay-compiler`, `babel-plugin-relay`; 2025-08-06) | — | lab 12.6; 21.0.1 was a day old. relay-compiler 20.1.1 panics on a `.graphqls` schema file name; Babel 7.29 + `@babel/preset-react` build it |
| Apollo Kotlin | 5.2.0 (2026-09-16), Gradle plugin `com.apollographql.apollo`, `apollo-normalized-cache-sqlite` | — | lab 12.7; **Gradle 9.9.1** (`gradle:9.9.1-jdk21`, `bin/course mobile`) and Kotlin 2.4.10, because the only Maven plugin (`com.github.aoudiamoncef:apollo-client-maven-plugin` 7.1.0, 2023) targets Apollo 3.8.2. No Android SDK |
| InQL, DVGA, OWASP ZAP | not run | run | lecture 10.8 describes them: InQL needs Burp's GUI, DVGA and ZAP are large images |
| GitHub Actions | actions/checkout v7, actions/setup-java v6 | — | Module 9 project workflow (Module 7's pins v4) |
| PostgreSQL JDBC | 42.7.14 (Boot BOM; also in course-lib) | — | single-file SQL labs (8.2) |
| jqwik | 1.10.2 | — | lab 8.4; runs on Boot 4.2's JUnit 6.1.3 platform with no extra config |
| Lanterna | 3.1.5 | — | mini-project 8; 3.2.0-alpha1 skipped. `DefaultVirtualTerminal.readInput` holds the terminal lock while waiting, so the app uses `pollInput` |
| Hibernate ORM / Spring Data JPA | 7.4.15.Final / 4.2.0 (Boot BOM) | — | Module 8 findings: OR-form keyset SQL, `cb.literal` inlined (`_workbench/2026-11-21-m08-probe/FINDINGS.md`) |
| grpc-java / protobuf | 1.84.1 / 4.36.2 (Boot BOM) | — | protoc via `io.github.ascopes:protobuf-maven-plugin` 5.1.12 |
| JUnit | 6.1.3 (Boot BOM) | — | |
| Hurl | 8.1.0 | — | |
| grpcurl | 1.9.4 | — | |
| Toxiproxy | 2.13.0 | — | |
| GraphiQL | 6.0.1 (bundled by Spring for GraphQL 2.1) | 5 | measured from `/graphiql` |
| picocli | 4.7.7 | — | lab 2.12 |
| maven-compiler / jar / dependency plugins | 3.16.0 / 3.5.1 / 3.11.0 | — | latest 3.x lines; 4.0.0 betas skipped |
| Javalin | 7.2.4 | — | lab 5.4 bonus, mini-project 6 |
| Caffeine | 3.3.1 | — | lab 5.8 document cache |
| slf4j-simple | 2.0.20 | — | Javalin logging |
| Quarkus platform | 3.40.1.2 (`quarkus-smallrye-graphql`) | — | Quarkus core 4.0.0 is out but its platform BOM is only 4.0.0.CR1 |
| GraalVM CE (native-image) | 25.0.2 (`ghcr.io/graalvm/native-image-community:25`) | — | `bin/course native`, game 6b; `docker/native.Dockerfile` adds the course's Maven |
| Querydsl | not used | 5 | `com.querydsl` 5.1.0 (2024-01-29) is the last release; fork `io.github.openfeign.querydsl` 7.7 |
| GraphQL over HTTP | September 2026 edition (2026-11-12) | draft | https://http-spec.graphql.org/September2026/ ; copy in `_workbench/2026-11-21-m05-probe/` |
| graphql-inspector | CLI 7.0.2 (2026-10-20), core 8.0.1, diff-command 7.0.2, graphql-js 16.12.0 | — | `bin/course inspector`, own image `docker/inspector.Dockerfile` (node 24.22.1-alpine); npm overrides stop caret ranges from pulling 7.0.8 (2026-11-20) |
| GitHub GraphQL schema snapshot | 2026-11-21 | — | `tools/schemas/github-2026-11-21.graphql`, for offline validation; refresh each term |
| Testcontainers in the course container | 2.0.5 (Boot BOM), Ryuk 0.14.0, `testcontainers-keycloak` 4.4.1 (2026-11-21) | — | Module 13.3; host Docker socket mounted only for labs that need `"docker"`; course images reused (postgres 18.7, kafka 4.4.0, keycloak 26.8.2) |
| Node.js inside the course image | 24.22.1, npm 11.19.1, copied from `node:24.22.1-slim` into `.cache/node` | — | Modules 13.6-13.7 (`bin/course node`, labs that need `"node"`); course image unchanged |
| graphql-eslint | `@graphql-eslint/eslint-plugin` 4.4.2 (2026-10-14), eslint 10.14.0 (2026-10-30), graphql 16.15.0 | — | lab 13.6; eslint 10.15.0 was 9 days old; graphql-eslint needs graphql ^16 |
| Spotless | `spotless-maven-plugin` 3.11.0 (2026-11-02) | — | lab 13.6; 3.12.0 was one day old |
| ArchUnit | `archunit-junit5` 1.5.1 (2026-09-25) | — | lab 13.6; reads Java 25 class files |
| WireMock | `wiremock-standalone` 3.13.2 (2025-11-14) | — | lab 13.4; the 4.x line is beta (4.0.0-beta.44); runs on JUnit 6.1.3 |
| PIT | `pitest-maven` 1.30.2 (2026-11-05), `pitest-junit5-plugin` 1.2.3 | — | game 13.8; the JUnit 5 plugin runs on Boot's JUnit 6.1.3 |
| SpectaQL | 3.0.9 (2026-03-18), graphql-config 5.1.8 | SpectaQL or Magidoc | lab 13.7; Magidoc 9.0.0 was 12 days old; graphql-config 5 matches graphql-eslint's |
| maven-shade-plugin | 3.6.2 | — | mini-project 13.10's standalone jar |
| Open Library (recorded) | responses of 2026-11-22, served by `modules/m14-patterns/contract/openlibrary/FakeOpenLibrary.java` | — | labs 14.4, 14.10; re-record each term (data changes, the API may too) |
| graphql-java-extended-scalars | 24.0 (`DateTime`, `Date`) | — | lab 14.2; no release newer than 24.0 on Central (2026-11-22) |
| Resilience4j circuit breaker | 2.4.0 | — | lab 14.4 (as Module 12) |
| Apollo Router | 2.19.0 (2026-10-22), Elastic License 2.0, run unlicensed | Apollo Router | Module 15; `bin/course fed` (sha256-checked into `.cache/fed`); subscriptions need a licence; 2.19.1 was 6 days old |
| Hive Router | 0.3.9 (2026-10-29), MIT | — | Module 15, second router; no `@defer`; 0.3.19 was 2 days old. Capstone reference: persisted documents (file), demand control, JWT (local JWKS), header propagation, SSE subscriptions |
| GitHub Actions (capstone reference CI) | checkout v7, setup-java v6, setup-node v7, upload-artifact v7 (majors; latest v7.0.1, v6.3.0, v7.1.0, v7.0.2 on 2026-11-23) | — | Read by the stage 4 checker, not run |
| federation-jvm | `federation-graphql-java-support` 7.0.0 (2026-08-03) | — | Module 15 subgraphs; not in Boot's BOM; 7.1.1 (2026-11-17) too new |
| Federation composition | `@theguild/federation-composition` 0.27.0 (2026-09-23, MIT), graphql 16.15.0 | Rover / Hive CLI | Module 15; supports federation v2.0 to v2.9; Rover's composition needs ELv2 acceptance (not accepted) |
| Nadel | `com.atlassian:nadel` 0.42134418907 (2026-11-06), Apache-2.0 | — | lab 15.2; brings its own graphql-java; 0.44642751879 (2026-11-20) probed, too new to pin |
| graphql-kotlin | 11.0.0 (2026-10-07), Kotlin 2.4.20 (`kotlin-maven-plugin`, 2026-09-07), maven-shade-plugin 3.6.2 | — | mini-project 15.10; federation link pinned to v2.9 (default v2.13) |
| GitHub Actions (Module 15) | `actions/checkout@v7`, `actions/setup-node@v7`, `actions/upload-artifact@v7` | — | lab 15.5 workflow, read by the test, not run |
| Prometheus | 3.16.0 (2026-11-05) | — | Module 16; `bin/course ops`, sha256-checked into `.cache/ops`, runs in the course image |
| Tempo | 3.1.1 (2026-10-21) | — | Module 16; single binary, paths set off `/var/tempo`; search lags 20–30 s |
| Grafana | 13.3.0 (2026-10-27) | — | Module 16; 13.3.1 (2026-11-10) too new; anonymous admin, reporting off |
| OpenTelemetry Java agent | 2.33.1 (2026-10-26) | — | Module 16.1 bonus, via Maven Central; 2.34.0 too new |
| Jib Maven plugin | 3.5.2 (2026-07-14) | Jib or Buildpacks | lab 16.4; `jib:buildTar` probed, image run on the host |
| Resilience4j bulkhead | 2.4.0 | — | lab 16.5, with the circuit breaker |
| Jackson YAML | `tools.jackson.dataformat:jackson-dataformat-yaml` 3.1.8 (snakeyaml-engine 3.0.1) | — | course-lib, Module 16 manifests |
| Jackson / slf4j-nop in standalone Module 15 jars | `tools.jackson.core:jackson-databind` 3.1.8 (2026-10-18); slf4j-nop 1.7.36 (Nadel's slf4j 1.x) / 2.0.17 (Kotlin) | — | lab 15.2 and mini-project 15.10, outside Spring Boot |
| GraphQL spec | September 2025 edition | — | Module 17 baseline; https://spec.graphql.org/September2025/ ; copy in `_workbench/2026-11-23-m17-probe/spec/` |
| Jazzer | `com.code-intelligence:jazzer-junit` 0.30.0 (2026-02-24), Apache-2.0 | — | lab 17.3 fuzz test; needs the JUnit BOM 6.1.3 import and `jazzer.instrument` |
| graphql-http | 1.23.1 (2026-09-28), MIT, graphql 16.15.0 | — | lab 17.8 audit, npm lockfile in `modules/m17-under-the-hood/contract/http-audit/`, course Node 24 |
| maven-surefire-plugin (Module 17) | 3.5.4 | — | the engine's test pom (fuzz test), outside Spring Boot |
| Spring AI | 2.0.2 (2026-10-22; BOM `spring-ai-bom`), built on Boot 4.1.2, runs on 4.2.0 | — | Module 18 MCP server (`spring-ai-starter-mcp-server-webmvc`); 2.0.3 and 2.1.0 (2026-11-19) too new |
| MCP Java SDK | 2.1.0 (from Spring AI 2.0.2) | — | negotiates MCP up to 2025-11-25; the current spec is 2026-07-28 |
| MCP protocol | 2025-11-25 (asked by `contract/mcp.sh`) | — | spec text read: 2025-06-18 (transports, elicitation, authorization) |
| PostGraphile | 5.2.1 (2026-11-02), MIT, graphql 16.15.0 | — | lab 18.4, npm lockfile in the lab |
| GraphQL Yoga / Pothos | graphql-yoga 6.0.1; @pothos/core 4.16.0, plugin-federation 4.5.4, plugin-directives 4.3.3 (ISC) | — | lab 18.6 TypeScript port; Node 24.22.1, TypeScript 6.0.3, @types/node 24.19.2 |
| @apollo/subgraph (with Pothos) | 2.14.4 | 2.17.0 latest | 2.15.0+ drops the `EntityType` export the Pothos federation plugin imports (Module 18 finding 4) |
| Viaduct | v3.0.0 (2026-10-21), Apache-2.0 | — | lab 18.4 rubric part; Gradle starter not run |
