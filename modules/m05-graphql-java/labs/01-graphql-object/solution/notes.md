# Lab 5.1 notes

## Timings (ms, median of 5 runs)

Measured in the course image on the build machine (2026-11-21); yours will differ, the shape should not.

| Query | parse | validate | execute |
|---|---|---|---|
| `missing-club.graphql` (2 fields) | 2.0 | 3.6 | 65 (first execution warms up the engine) |
| 5,000 aliased `hello` fields | 23.5 | 25.2 | 33.4 |

## Questions

1. Parse and validate grow roughly linearly with the document: the parser builds one AST node per token
   and validation walks every selection with ~30 rules, including the overlapping-fields rule, which
   compares fields with the same response name. Execute for `hello` is cheap per field, so for trivial
   fetchers the front half of the pipeline dominates. With real fetchers execution dominates instead.
2. "More than 15,000 'grammar' tokens have been presented. To prevent Denial Of Service attacks, parsing
   has been cancelled." `ParserOptions.maxTokens` defaults to 15,000. A tiny HTTP body can otherwise make
   the server burn CPU in the parser before any limit on depth or cost even runs (Module 10).
3. A `PreparsedDocumentProvider` that returns my own `PreparsedDocumentEntry(doc)` without calling the
   `parseAndValidate` function. graphql-java then trusts the entry and skips validation. In a server that
   is only safe if the cached document really was validated against the same schema; caching an
   unvalidated document would execute invalid queries.
4. `syntax.graphql` is a request error: the request never reached execution, so `data` is absent and only
   `errors` is present. `missing-club.graphql` executed: `data` is an object, `club` inside it is `null`
   (a nullable field that resolved to nothing) and there is no `errors` key. `data` itself would be
   `null` only if a field error propagated up through non-null fields to the root.
