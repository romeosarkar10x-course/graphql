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
| extended-scalars / extended-validation | 24.0 / 24.0 | — | check compatibility with graphql-java 26 in Module 3 |
| Testcontainers | 2.0.5 | — | |
| grpc-java / protobuf | 1.84.1 / 4.36.2 (Boot BOM) | — | protoc via `io.github.ascopes:protobuf-maven-plugin` 5.1.12 |
| JUnit | 6.1.3 (Boot BOM) | — | |
| Hurl | 8.1.0 | — | |
| grpcurl | 1.9.4 | — | |
| Toxiproxy | 2.13.0 | — | |
| GraphiQL | 6.0.1 (bundled by Spring for GraphQL 2.1) | 5 | measured from `/graphiql` |
