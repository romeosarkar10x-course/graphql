# Language port guide

The course is written so only the build column of each lab changes when you switch language: the `.graphql` contract, the Hurl conformance tests and the behaviour README stay identical. This guide maps each Java tool to its closest equivalent so a module can be redone in another backend language.

## How to port a module

1. Copy the module's `contract/` folder (schema plus operations) unchanged
2. Re-implement the server or client with the libraries below
3. Run the module's Hurl suite; it must pass without edits
4. Re-run the module's experiment and compare numbers with the Java baseline
5. For federation modules, the ported subgraph must compose into the same supergraph

Modules 1, 2, 7 and 15 are mostly language-neutral already. Modules 5, 6, 9, 10, 11, 12, 13, 16 and 17 need real re-implementation. Suggested order after Java: Kotlin (same JVM), Go, TypeScript, Python, C#, Rust.

## Java, Go, TypeScript, Python

| Concern | Java (this course) | Go | TypeScript / Node | Python |
| --- | --- | --- | --- | --- |
| Core engine | graphql-java | gqlgen, graph-gophers/graphql-go | graphql-js | graphql-core |
| Server framework | Spring for GraphQL, DGS, Quarkus SmallRye | gqlgen + `net/http` | GraphQL Yoga, Apollo Server, Mercurius | Strawberry, Ariadne, Graphene |
| Schema style | Schema-first (Spring, DGS); code-first (Quarkus, your generator) | Schema-first with codegen (gqlgen) | Code-first with Pothos; schema-first with graphql-tools | Code-first (Strawberry); schema-first (Ariadne) |
| Batching | java-dataloader, `@BatchMapping` | dataloadgen, graph-gophers/dataloader | dataloader | Strawberry DataLoader |
| Subscriptions | Spring WebSocket and SSE transports, Reactor | gqlgen websocket and SSE transports | graphql-ws, graphql-sse | Strawberry WebSocket support |
| `@defer` / `@stream` | graphql-java experimental `@defer` | Check gqlgen release notes | graphql-js 17 (experimental API) | Strawberry experimental (graphql-core 3.3 alpha) |
| Federation subgraph | federation-jvm, `@EntityMapping`, DGS | gqlgen federation plugin | `@apollo/subgraph`, Yoga federation | Strawberry federation |
| Demand control | graphql-java limits and instrumentation | gqlgen complexity extension | GraphQL Armor | Strawberry query-depth limiter |
| Server tests | JUnit, `GraphQlTester`, Testcontainers | `testing`, `httptest`, testcontainers-go | Vitest, supertest, Testcontainers | pytest, Testcontainers |
| Typed client | Spring `GraphQlClient`, DGS codegen client | genqlient | GraphQL Code Generator, gql.tada, Apollo Client, urql, Relay | gql, ariadne-codegen |
| Parser / AST (Module 17) | graphql-java `Parser` | vektah/gqlparser | graphql-js `parse` | graphql-core `parse` |
| Observability | Micrometer, OpenTelemetry Java agent | OpenTelemetry Go | OpenTelemetry JS, Envelop plugins | OpenTelemetry Python |

## Kotlin, C#, Rust

| Concern | Kotlin | C# / .NET | Rust |
| --- | --- | --- | --- |
| Core engine | graphql-java | Hot Chocolate, graphql-dotnet | async-graphql, Juniper |
| Server framework | DGS, Spring for GraphQL, graphql-kotlin, Viaduct | Hot Chocolate on ASP.NET Core | async-graphql with axum |
| Schema style | Code-first (graphql-kotlin) or schema-first (DGS) | Code-first or schema-first (Hot Chocolate) | Code-first via derive macros |
| Batching | java-dataloader, DGS loaders | GreenDonut (Hot Chocolate) | async-graphql DataLoader |
| Subscriptions | Same as Java, plus coroutines and `Flow` | Hot Chocolate WebSocket and SSE | async-graphql WebSocket |
| Federation | federation-jvm, graphql-kotlin federation | Hot Chocolate Fusion or Apollo Federation support | async-graphql federation |
| Demand control | graphql-java limits | Hot Chocolate cost analysis | `limit_depth`, `limit_complexity` |
| Typed client | Apollo Kotlin, graphql-kotlin clients | Strawberry Shake | cynic, graphql-client |
| Parser / AST | graphql-java `Parser` | Hot Chocolate language package | apollo-parser, apollo-compiler |

## Idiom notes when porting

- **Sealed types and unions:** Java sealed interfaces map to Kotlin sealed classes, Go interfaces with a type switch, TypeScript discriminated unions, Python `Union` types, C# records with pattern matching, Rust enums
- **Concurrency:** virtual threads and `CompletableFuture` become goroutines, `async`/`await` in TypeScript, Python and C#, and Tokio futures in Rust; DataLoader batching windows differ per runtime, so re-run the Module 9 experiment
- **Null safety:** JSpecify in Java maps naturally to Kotlin, TypeScript strict mode, C# nullable references and Rust `Option`; Go and Python need extra discipline
- **Code generation:** Go (gqlgen) and Rust lean on codegen and macros; TypeScript has the richest client codegen

Library names reflect each ecosystem as of November 2026 and may change; verify against each project's docs before a port, and pin versions per language in the course repo.
