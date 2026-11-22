# Module 13 probe findings (2026-11-22)

Everything below was run in this folder or by the labs' tests; files are named per finding.

1. **Testcontainers 2.0.5 works from inside the course container** with the host's Docker socket mounted and the
   socket's group added (`--group-add`), on the `gqlcourse` network, with no Testcontainers settings: it reached the
   containers through the network's gateway (10.100.0.1 here). Ryuk 0.14.0 started. Start times, one machine:
   PostgreSQL 0.9 s, Kafka 4.4.0 2.0 s, Keycloak 26.8.2 10.2 s (11.5 s with the client). `tc/`, `tc/probe-run.txt`.
2. **Node.js copied from `node:24.22.1-slim`** (`/usr/local/bin/node` plus `lib/node_modules`) runs in the course
   image (Ubuntu noble, glibc 2.39): node 24.22.1, npm 11.19.1. 140 MB. Now `bin/course node` and `.cache/node`.
3. npm 11.19.1 does not run dependencies' install scripts unless approved (`npm warn install-scripts`, here
   `@parcel/watcher` from SpectaQL); SpectaQL builds anyway. `spectaql/`.
4. **`@GraphQlTest` still reads `@Bean` methods on the `@SpringBootApplication` class**: a test `Clock` bean failed
   with "A bean with that name has already been defined in dev.shelfie.app.ShelfieApplication and overriding is
   disabled". The slice uses the application's clock.
5. **`ExecutionGraphQlServiceTester` skips `WebGraphQlInterceptor`s**: a mutant that reads the wrong header
   survived every in-process test and failed only the `HttpGraphQlTester` (MockMvc) tests (lab 13.2, G6).
6. A hot `Sinks.many().multicast().directBestEffort()` feed loses events published before the WebSocket
   subscription is registered on the server; `StepVerifier.then` runs after the client subscribes. The app exposes
   `liveSubscribers()` and the test waits on it; three runs in a row passed.
7. **Bearer tokens through `@AutoConfigureHttpGraphQlTester` (MockMvc)**: with Spring Security's resource server, a
   valid Keycloak token still gave `UNAUTHORIZED` "Unauthorized" in the controller, and security debug logging
   showed no request through the filter chain; the same test on a real port (`RANDOM_PORT`, `WebTestClient`)
   authenticated. Root cause not investigated.
8. **Module 5's server and Spring against the same suites** (lab 13.3's run): Module 6's 14 behaviour checks and a
   10-entry black-box contract pass on both; Module 5's 25-check GraphQL-over-HTTP audit: 25 of 25 on the
   graphql-java server, 11 of 25 on Spring for GraphQL (as Module 6 found).
9. **graphql-java's `MockedWiringFactory` answers null everywhere**: `club(id:)` is null, a non-null `search` is a
   `NullValueInNonNullableField` error. It makes a schema executable, nothing more. `mockedwiring/`.
10. **MockSpec** (rfcs.graphql.org/rfcs/MockSpec, RFC 0 strawman, opened 2026-02-02, updated 2026-11-21): client-side
    `@mock(name:)` on operations, `__graphql_mocks__/<OperationName>.json` with `__default__` and named responses,
    `__description__`, auto-generation of a missing default, "must raise an error indicating the available mock
    names", validation by running the operation with no resolvers and the mock as root value. `mockspec.txt`.
11. **WireMock 3.13.2** (`wiremock-standalone`, 2025-11-14; 4.x is still beta) runs with `@WireMockTest` on Boot's
    JUnit 6.1.3.
12. **graphql-java 26.1 `SchemaDiff`** reports every lab 13.5 scenario: MISSING (field, enum value), INVALID (type
    change), STRICTER (argument made non-null, required input field), ADDITION (enum value, DANGEROUS), DIFFERENT
    (default, DANGEROUS), DEPRECATION_ADDED (DANGEROUS). The default-value message is cut off: "...on field
    'Club.members' of type 'Club". `getTypeKind()` is an enum, not a String. `schemadiff/events.txt`.
13. **graphql-inspector 7.0.2 disagrees with SchemaDiff** on deprecation (✔ safe against DANGEROUS) and on a new
    non-null argument with a default (⚠ dangerous against BREAKING); it prints the default change as "from 10 to 5".
    `schemadiff/inspector-scenarios.txt`, `schemadiff/break-the-build-inspector.txt`.
14. **SchemaDiff's false positive**: adding `sortBy: String! = "title"` is BREAKING to SchemaDiff, but an old query
    without it validates and the resolver receives "title" (spec: Required Arguments). `schemadiff/Round06.java`.
15. `QueryTraverser` coerces variables and throws `NonNullableValueCoercedAsNullException` for an operation with a
    non-null variable and no values; lab 13.5's usage walk is hand-written instead.
16. The GitHub Action is `graphql-hive/graphql-inspector@master` (the repository moved from kamilkisiela), inputs
    `schema`, `fail-on-breaking`, `annotation-level`, `rules`, `onUsage`... (`action.yml`, `action.mdx`). Not run:
    the course has no GitHub Actions runner.
17. **graphql-eslint 4.4.2 `flat/schema-recommended`** on Shelfie's described schema: 66 errors, 64
    `description-style` (wants block descriptions), 2 `strict-id-in-types` (payload types). Rules that look at the
    whole schema fail to load without `parserOptions.graphQLConfig.schema` ("requires graphql-config `schema`
    field"). ESLint 10 ignores files outside the config's directory, so the config sits at the project root.
    `eslint-recommended-on-clean.txt`.
18. **ArchUnit 1.5.1 reads Java 25 class files**; `layeredArchitecture()` allowed a controller to call a
    repository (graph may use store), a rule on `@Repository`-annotated dependencies caught it.
19. **graphqlcodegen-maven-plugin 3.11.0** generates `DgsConstants` with `<TYPE>.<Field>` and
    `<FIELD>_INPUT_ARGUMENT.<Arg>` constants and JavaBean input types; Spring binds the generated
    `CreateReviewInput` with no extra config. No graphql-java 25 pin needed (no DGS framework at run time).
20. **A schema rename without codegen** compiles and starts; Spring logs `Unmapped fields: {Book=[avgRating]}` and
    `Unmapped registrations: {Book.averageRating=...}` at INFO and serves `avgRating: null`.
    `rename-without-codegen.txt`.
21. **SpectaQL 3.0.9** builds Shelfie's docs from the SDL file in about a second: one 110 KB `index.html` with
    `oneFile: true`, anchors `definition-<Type>`, `query-<field>`, `mutation-<field>`, `subscription-<field>`.
22. **PIT 1.30.2 + pitest-junit5-plugin 1.2.3 run on JUnit 6.1.3** (Boot BOM): 69 mutants in the two services;
    lab 13.1's reference tests kill 62 (89%, 2 survive in the feed's filter lambda, 5 not covered), the starter's
    one test 11 (15%). Mockito warns about self-attaching its agent. `pit/`.
