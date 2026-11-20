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
| Netflix DGS | 12.1.1 | 11 | update Module 6.7 text when built |
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
| grpc-java / protobuf | 1.84.1 / 4.36.2 (Boot BOM) | — | protoc via `io.github.ascopes:protobuf-maven-plugin` 5.1.12 |
| JUnit | 6.1.3 (Boot BOM) | — | |
| Hurl | 8.1.0 | — | |
| grpcurl | 1.9.4 | — | |
| Toxiproxy | 2.13.0 | — | |
| GraphiQL | 6.0.1 (bundled by Spring for GraphQL 2.1) | 5 | measured from `/graphiql` |
| picocli | 4.7.7 | — | lab 2.12 |
| maven-compiler / jar / dependency plugins | 3.16.0 / 3.5.1 / 3.11.0 | — | latest 3.x lines; 4.0.0 betas skipped |
| GitHub GraphQL schema snapshot | 2026-11-21 | — | `tools/schemas/github-2026-11-21.graphql`, for offline validation; refresh each term |
