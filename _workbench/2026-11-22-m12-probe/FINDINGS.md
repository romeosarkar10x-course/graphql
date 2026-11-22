# Module 12 probe findings (2026-11-22)

Spring Boot 4.2.0, Spring for GraphQL 2.1.0, graphqlcodegen-maven-plugin 3.11.0 (DGS codegen shared-core 8.8.0),
Quarkus platform 3.40.1.2 (SmallRye GraphQL client), Resilience4j 2.4.0, JLine 4.7.0, Toxiproxy 2.13.0,
Apollo Client 4.2.12, Relay 20.1.1, React 19.2.9, jsdom 27.4.0, Node.js 24.22.1, Apollo Kotlin 5.2.0,
Kotlin 2.4.10, Gradle 9.9.1 (JDK 21). Raw output in this folder; probe projects in `typesafe/`, `spring/`,
`quarkus/`; the scenario generator in `cache/`.

## Spring's clients (lab 12.2)

1. **`documentName("clubDigest")` loads the file but sends no `operationName`**: a client interceptor saw
   `getOperationName() == null` for every call until `.operationName(...)` was set. (lab 12.2 test, `/stats`)
2. `retrieveSync("club").toEntity(...)` returns `null` for a null field with no errors (reactive: an empty
   `Mono`); a field that does not exist throws `FieldAccessException: Invalid field 'club', errors:
   [{message=Validation error (FieldUndefined@[club/nope]) ...}]`. (`spring-probe.txt`)
3. **The course image's `SHELFIE_URL` environment variable overrode `@Value("${shelfie.url}")`** (relaxed
   binding) and every call went to `http://shelfie-ref:8080` without `/graphql`: 404. The lab uses
   `digest.shelfie-graphql`.
4. `WebSocketGraphQlClient` reports a refused connection as `GraphQlTransportException: GraphQlTransport
   error: finishConnect(..) failed ... Connection refused`; with `retryWhen(Retry.backoff(...))` the
   subscription came back after the chat server restarted, and the messages sent meanwhile were lost.
5. **`HttpSyncGraphQlClient` does not wrap HTTP transport failures**: a refused connection is RestClient's
   `ResourceAccessException` (cause `ConnectException`), a 404 is `HttpClientErrorException$NotFound`; neither
   is a `GraphQlTransportException`. Retry configured for `GraphQlTransportException` retried nothing.
   (`exception-types.txt`)

## Type-safe clients (labs 12.3, 12.4)

6. **`DgsGraphQlClient` with codegen's client API needs no DGS framework** (only
   `graphql-dgs-codegen-shared-core`), so Module 6's graphql-java 25 pin does not apply. Two root fields
   (`club` and `books`) went in one HTTP request (`/_course/stats`: 1).
7. **The generated requests inline arguments as literals and have no operation name**:
   `{ club(id: "c1") { name currentBook { title reviews(last: 2) { rating } } } }`. (`dgs-serialized.txt`)
8. Codegen wrote 46 classes in `client/` and 27 in `types/` for the 30-type reference schema. Renaming
   `Book.title` broke `ClubPage.java` at compile time in 3 places (`cannot find symbol: method title()`,
   `getTitle()`). (`rename-compile.log`)
9. **SmallRye's typesafe client declares a `String` parameter annotated `@Id` as `ID`**, nullable; Shelfie
   refused it: `Variable 'id' of type 'ID' used in position expecting type 'ID!'` (VariableTypeMismatch).
   `@NonNull` fixes it; a primitive `int` is already `Int!`. (`quarkus-probe-nullable-id.txt`)
10. The typesafe client sends variables and the method name as `operationName`; `@NestedParameter
    ("currentBook.reviews")` puts an argument on a nested field. **The dynamic client sends no
    `operationName`**, even for a named operation (`query clubCard(...)`). (`quarkus-wire.txt`, `quarkus-nested.txt`)

## Resilience (lab 12.8)

11. **Toxiproxy applies toxicity per connection.** A `reset_peer` toxic at 0.3 hit 1 of 100 requests through a
    keep-alive client and 26 of 100 with a new client per call; `-Djdk.httpclient.keepalive.timeout=0` gave 65 to
    70 failures in 200. (`toxi-probe.txt`)
12. **A downstream `reset_peer` loses responses after Shelfie acted**: 10 `createReview` calls, the client saw 4
    transport failures, Shelfie stored all 10; replaying one answered `DUPLICATE`. (`toxi-probe.txt`)
13. Reference client, four runs (30% lost responses, 5% hung connections, 500 ms timeouts): one attempt
    65–71% of 200 queries; five attempts with jittered backoff 99–100%; with the proxy disabled the circuit
    breaker refused 98 of 100 calls, 1.3–1.6 s in all. 3 to 6 of 10 reviews per run were recovered after a lost
    response. (`resilience-runs.txt`)

## The normalised cache (lab 12.5, mini-project 12.9)

14. The reference cache is 208 lines for scenarios 01–10 and 462 for all 25. Against Shelfie, a book page and
    a club page both showing b1's average went from 4.2 to 3.67 after one `createReview` whose response selected
    `book { __typename id averageRating }`, with 3 requests in all. (`cache/live-demo.txt`)

## Web clients (lab 12.6)

15. **Re-renders after one mutation** (club card with 8 member rows): Apollo Client with props: ClubCard 1,
    BookPanel 1, MemberRow 8. With `React.memo`: 1, 1, 0. Apollo Client 4 with data masking: 0, 1, 0. Relay: 0, 1,
    0. All four showed the new average with 2 requests. (`web-renders.txt`)
16. **relay-compiler 20.1.1 panics** (`no entry found for key`, `build_schema.rs:115`) when the schema file ends
    in `.graphqls`; `.graphql` works. babel-plugin-relay imports artifacts without an extension, which Node's
    ES module loader refuses (a `registerHooks` resolve hook fixes it); react-relay is CommonJS, so named imports
    from an ES module fail (`Named export 'graphql' not found`).

## Mobile clients (lab 12.7)

17. **No current Maven plugin for Apollo Kotlin**: `com.github.aoudiamoncef:apollo-client-maven-plugin` 7.1.0
    (2023-12-30) targets Apollo 3.8.2. Lab 12.7 uses Gradle in the official image.
18. Airplane mode (proxy disabled) with the SQLite cache: `NetworkOnly` failed with an `ApolloNetworkException`
    in `response.exception`, `CacheOnly` in a new process showed the card; a queued review synced afterwards and
    the cached card showed 3.67. **Without `@typePolicy(keyFields: "id")` the cached card still showed 4.2 after
    the sync** (records keyed by path). (`mobile-airplane.txt`, `mobile-airplane-no-keyfields.txt`)

## The CLI (mini-project 12.10)

19. JLine 4.7.0 on Java 25 prints a restricted-method warning (`MemorySegment::reinterpret` from
    `FfmTerminalProvider`) unless the jar's manifest has `Enable-Native-Access: ALL-UNNAMED`. With stdin piped
    it falls back to a dumb terminal and reads lines; under `script` (a pty) Tab completed `ti` to `title`.
