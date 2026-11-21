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
