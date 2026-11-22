# Reviews in TypeScript

Reference notes for the solution in `src/server.ts` (GraphQL Yoga 6.0.1, Pothos 4.16.0, @pothos/plugin-federation
4.5.4, @apollo/subgraph 2.14.4, graphql 16.15.0, Node 24.22.1). Probes in `_workbench/2026-11-23-m18-probe/port/`.

## What did not carry over

- **Nullability.** Pothos makes every field nullable by default; the first `_service` SDL had `id: ID`,
  `[Review!]` and `author: User`. `defaultFieldNullability: false` (and `DefaultFieldNullability: false` in the
  builder's type parameters) flips it, and the three nullable fields (`review`, `addReview`, `averageRating`) say
  `nullable: true`. The signature check caught it; Hurl would not have, because the data was the same.
- **Argument order.** Introspection lists `addReview`'s arguments as `body, bookId, rating` (alphabetical) while
  Java keeps `bookId, rating, body`. Named arguments make that harmless, so the contract compares them sorted.
- **Federation directives.** `externalRef` types are printed with `@extends`; listing the directives by hand
  (`federationDirectives: ['@key', '@external', '@provides']`) left `@extends` out of the `@link` import and the
  schema failed to build ("Unknown directive @extends"). Letting Pothos compute the list fixed it. Pothos links
  Federation v2.6 by default; the course's composer and the Java subgraphs use v2.9, so `linkUrl` says so.
- **A dependency that broke in a minor release.** `@apollo/subgraph` 2.15.0 (2026-08-28) stopped exporting
  `EntityType` from `dist/types.js`, which `@pothos/plugin-federation` 4.5.4 imports, so Node refused to load the
  plugin (`SyntaxError: ... does not provide an export named 'EntityType'`). The plugin declares
  `"@apollo/subgraph": "^2.0.0"`, so npm saw no conflict. Pinned to 2.14.4, the newest 2.14 (2.14.x still exports
  it; the plugin's own tests use 2.13.3).
- **Errors.** Spring's `ErrorType.UNAUTHORIZED` is `extensions.classification`; a `GraphQLError` thrown in the
  resolver with the same extension produces the same JSON, and Yoga passes it through unmasked because it is a
  `GraphQLError`.
- **`@key` printing.** Java prints `@key(fields : "id", resolvable : true)`, Pothos `@key(fields: "id")`; the
  first version of `port.hurl` matched only the second, and the Java baseline caught it.

## Idioms

| Concern | Spring for GraphQL (Module 15) | Pothos + Yoga |
| --- | --- | --- |
| Schema | `reviews.graphqls`, schema first | built in code: `objectRef`, `implement`, `queryType` |
| Resolvers | `@QueryMapping`, `@SchemaMapping` | `resolve` functions next to each field |
| Entities | `@EntityMapping` with `List<String> idList` | `asEntity(ref, { key, resolveReference })`, one call per representation |
| Extended types | `type Book @key(fields: "id")` in the SDL | `externalRef('Book', selection, resolve)` |
| The reader | `WebGraphQlInterceptor` puts `userId` in the GraphQL context | Yoga's `context` function reads the header |

One difference matters for Module 15's batching lessons: Pothos's `resolveReference` is called once per
representation, where Spring's `@EntityMapping` receives the whole list, so a port that talks to a database needs
a DataLoader to keep "one store query".

## Numbers

1,000 requests, 4 at a time, `{ latestReviews(first: 5) { id rating body author { handle } book { id } } }`,
after 200 warm-up requests, M15's `Load.java`, one machine (`numbers.txt`).

| | Java (Module 15 contract jar, `-XX:TieredStopAtLevel=1 -Xmx192m`) | TypeScript (built, `node dist/server.js`) |
| --- | --- | --- |
| Startup to first answer | 986 ms | 170 ms |
| Memory (RSS after the run) | 173 MB | 141 MB |
| p50 / p95 / p99 | 0 / 1 / 2 ms | 0 / 1 / 3 ms |

At this size both are noise-level fast; startup is the visible difference. The Java figure is with the C1-only flag
the course uses for many small JVMs.

## Keep it?

Only if a team that writes TypeScript owns reviews. The contract made the port cheap to verify, but every
language is another set of dependencies to keep pinned (the `@apollo/subgraph` break above shows the cost), another
CI image, and another way to do DataLoader, errors and tracing. A polyglot graph is a team-structure decision:
federation lets each team pick, and the Hurl contract keeps them honest.
