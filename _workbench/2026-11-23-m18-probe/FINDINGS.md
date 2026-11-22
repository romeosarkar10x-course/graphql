# Module 18 probe findings (2026-11-23)

## Versions (Maven Central and npm, release dates; pins at least two weeks old)

1. **Spring AI 2.0.2** (2026-10-22; 2.0.3 and 2.1.0 came out on 2026-11-19). Built on Boot 4.1.2; runs on the
   course's Boot 4.2.0 (`mcp-server/probe1.txt`). Its MCP server uses **MCP Java SDK 2.1.0**
   (`spring-ai-mcp` 2.0.2's pom). Starter: `spring-ai-starter-mcp-server-webmvc`.
2. **MCP protocol versions**: the 2.0.2 stack negotiates 2024-11-05, 2025-03-26, 2025-06-18 and 2025-11-25; asked
   for 2026-07-28 (the current version per modelcontextprotocol.io/specification/versioning), it answers
   2025-11-25 (`mcp-server/versions.txt`). `contract/mcp.sh` asks for 2025-11-25 and sends
   `MCP-Protocol-Version` afterwards (required by the 2025-06-18 transport text, `mcp-spec/basic-transports.txt`).
3. **npm**: graphql-yoga 6.0.1 (2026-10-23), @pothos/core 4.16.0 (2026-10-22), @pothos/plugin-federation 4.5.4
   (2026-10-22), @pothos/plugin-directives 4.3.3, graphql 16.15.0 (2026-11-04), postgraphile 5.2.1 (2026-11-02,
   MIT), typescript 6.0.3, @types/node 24.19.2. MCP Inspector's licence field is "SEE LICENSE IN LICENSE": not
   used.
4. **@apollo/subgraph 2.15.0 breaks @pothos/plugin-federation 4.5.4**: 2.15.0 (2026-08-28) no longer exports
   `EntityType` from `dist/types.js`; the plugin's ESM build imports it, so Node fails with `SyntaxError: The
   requested module '@apollo/subgraph/dist/types.js' does not provide an export named 'EntityType'`. Tarballs
   checked: 2.13.3, 2.14.0, 2.14.1, 2.14.4 export it, 2.15.0 does not. The plugin's peer range is `^2.0.0`; its
   devDependency is 2.13.3. Pinned 2.14.4 (`port/apollo-subgraph-2.15.txt`).
5. **Viaduct v3.0.0** (2026-10-21, Apache-2.0); starter `viaduct-dev/cli-starter` at d111e575 (2026-10-21), Gradle
   wrapper 9.1.0. **Not run**: running its `./gradlew` (inside the course's Gradle image) was denied in the
   authoring session, as graphql-java's was for Module 17 (finding 22 there). The starter sources are kept in
   `viaduct-cli-starter/` (without `.git`).
6. Platform repositories (GitHub API, 2026-11-23): hasura/graphql-engine Apache-2.0, v2.51.9 (2026-11-19);
   supabase/pg_graphql Apache-2.0, v1.6.3 (2026-11-02); neo4j/graphql Apache-2.0; dgraph-io/dgraph Apache-2.0,
   v25.5.1 (2026-10-22); ExpediaGroup/graphql-kotlin Apache-2.0, 11.1.0 (2026-11-22); graphile/crystal MIT.

## MCP server (labs 18.2, 18.3, 18.7)

7. Streamable HTTP at `/mcp`: `initialize` answers JSON with `Mcp-Session-Id`; later responses are SSE
   (`id:`, `event:message`, `data:`). Sync `@Tool` methods run on the servlet thread (`http-nio-...-exec-N`), so
   `RequestContextHolder` sees the MCP request's headers; `ToolContext` holds only `exchange`.
8. `@Tool` parameters become a JSON Schema with `additionalProperties: false`; a missing required parameter or an
   extra one is a tool error (`isError: true`) before the method runs. An unknown tool is a JSON-RPC error
   (-32602). An exception thrown in a tool becomes `isError: true` with the message as text.
9. **Spring Boot relaxed binding**: the environment variable `SHELFIE_URL` (set in the course image) binds onto
   a property named `shelfie.url` and overrode `${SHELFIE_URL:...}/graphql`, so every call went to `/` (404).
   The labs use `shelfie.graphql-url`.
10. **Unknown session**: `Mcp-Session-Id: bogus` gets HTTP 404 (the transport spec's rule) with a body that is a
    serialized Java exception including `stackTrace` (`mcp-server/probe2.txt`). Lecture and lab 18.3 mention it.
11. Lab 18.3's guard refusals word for word: `mcp-server/guard-messages.txt` (fan-out cost 5,101; variable default
    1,000; three aliases 300; depth 8).
12. Red team: the 18.7 starter (seven seeded weaknesses) blocks 6 of 12 course attacks, the solution 12 of 12
    (`mcp-server/redteam-starter.txt`). The first version of A03 was blocked by accident (the thief was already a
    member, so the join changed nothing); it now uses two readers in neither club and requires the confirm to be
    refused.

## Agents (lab 18.1)

13. Module 10's cost of the ten operations: 7 to 53 (`FindBooks` 30 with its `$first = 5`; priced with the
    variable's default, not the argument's 20). Reference queries: 10/10 answered, cost 1,527. The authoring
    sample (Claude Opus 5.5, having seen the data): 10 valid, 9 answered, cost 835; the miss was `meetups` without
    `upcomingOnly: false` (`agent/llm-sample-score.md`).
14. Reference server facts the descriptions state, checked: `books(first: 5000)` returns all 10 (no cap);
    a second review of the same book is `DUPLICATE` in `userErrors`; an unknown club is `NOT_FOUND`; no header is
    `UNAUTHENTICATED`; a request with `"onError": "NULL"` is answered normally (200) (`rfcs/onError-ref.txt`).

## PostGraphile (lab 18.4)

15. Amber preset on Module 9's club data: 20 Query fields (including the loader's `pick` function), 27 mutations
    (`postgraphile/probe1.txt`). `schema.defaultBehavior: "-insert -update -delete"` removes the mutation type;
    `makeJSONPgSmartTagsPlugin` with `procedure.pick.tags.behavior: "-*"` removes `pick`
    (`probe-hardened.txt`).
16. `orderBy`/`condition` only on indexed columns: `ReviewOrderBy` has no `RATING_*`; `ReviewCondition` has
    `rowId, bookId, authorId, createdAt` (`cursor.txt`). No page cap (`allReviews(first: 1000)` returns 1,000).
    Relation fields are nullable even over NOT NULL foreign keys (`readerByOwnerId: Reader`); `rowId: BigInt!`
    (`types.txt`). Ruru (GraphiQL) is at `/`, not `/graphiql`.
17. No depth or cost limit among grafserv/PostGraphile's options (type definitions searched: `maxRequestLength`,
    `maskError`, `graphqlOverGET`, ... only).
18. **A mutation on the read-only API** is not rejected by validation: graphql-js 16.15 has no rule for the
    September 2025 edition's 5.2.1.1 Operation Type Existence; Grafast fails at planning ("No mutation type found
    in schema", logged) and the client gets a masked "An error occurred (logged with hash: ...)", HTTP 400 with the
    default Accept.

## Port (lab 18.6, game 18.8)

19. Java's reviews subgraph passes `contract/port/port.hurl` (19 requests) unchanged (`port/java-baseline.txt`);
    so does the TypeScript port (`port/ts-run.txt`). Differences the contract caught: Pothos's nullable default,
    `@extends` missing from a hand-written import list, alphabetical argument order in introspection (compared
    sorted), Java's `@key(fields : "id", resolvable : true)` spelling, Hurl 8.1 having no array literals.
20. `@apollo/subgraph`'s `entitiesResolver` maps representations one by one (`representations.map`), so Pothos's
    `resolveReference` runs once per representation.
21. Numbers, one machine, 1,000 requests at concurrency 4 after 200 warm-up (`port/numbers.txt`): Java (contract
    jar, `-XX:TieredStopAtLevel=1 -Xmx192m`) 986 ms to first answer, 173 MB RSS, p50/p95/p99 0/1/2 ms; TypeScript
    (Node 24.22.1) 170 ms, 141 MB, 0/1/3 ms.
22. Game 18.8: Java catalog and clubs, Kotlin accounts (lab 15.10's solution), TypeScript reviews (18.6's
    solution) compose and pass the five cross-subgraph queries.

## RFCs (lab 18.5, lecture 18.5)

23. Tracker on 2026-11-23 (`rfcs/tracker-2026-11-23.tsv`): #1110 incremental delivery RFC 2; #1163 error
    behaviors RFC 1; #1081 fragment arguments RFC 2; #1208 service capabilities RFC 1; FullSchemas RFC 0;
    CompositeSchemas RFC 0 (its document last updated 2022-07-22; the work lives in the composite-schemas-spec
    repository). Persisted documents over HTTP: graphql-over-http PR #264 (open, last updated 2026-07-22); the old
    `rfcs/PersistedOperations.md` says it moved to an appendix, which is not on main and not in the September 2026
    edition (`2026-11-21-m05-probe/GraphQLOverHTTP-September2026.md` has no such appendix).
24. Merged into the spec's main branch since the September 2025 edition: #1193 (2026-02-05), #1206 (2026-06-04),
    #1227, #1211, #1183 (2026-09-17); the tracker also lists #1229 (RFC 1) as "Spec PR merged on 2026-11-05".
25. graphql-java 26.1 has no `onError` and no `__service`; it has `@experimental_disableErrorPropagation` on
    QUERY, MUTATION and SUBSCRIPTION behind an enable flag (`graphql/Directives.class` strings).
