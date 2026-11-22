# Lab 14.7 notes

## A column added, a field appears

The grader added a column and a metadata row with plain SQL; within about a second `Book.language` was in
introspection and answering, and the process was the same one. The schema is built in code from the metadata
(`GraphQLObjectType.newObject()` per type, a data fetcher per field reading its column) into a new `GraphQL`
instance, and an `AtomicReference` swaps it whole. Spring asks the `GraphQlSource` for its `GraphQL` on every
request, so each request runs on one schema from start to end: 150 queries during six swaps all succeeded. A poll
of a metadata fingerprint every second is the simplest trigger; PostgreSQL `LISTEN/NOTIFY` would cut the delay.

## Two endpoints in one JVM

`/admin/graphql` is assembled by hand the way Boot assembles `/graphql`: a `GraphQlSource` from its own schema
file, a `DefaultExecutionGraphQlService`, a `WebGraphQlHandler`, a `GraphQlHttpHandler`, and a route. It is not a
bean, so the auto-configuration keeps serving the public endpoint from the dynamic source. Separate schemas mean
the public API has no mutations to discover, and the admin key is checked before GraphQL parses anything.

## When a schema should not be dynamic

Clients generate code from schemas, and a field that appears at run time has no client yet; one that disappears
breaks them with no build to warn anyone (Module 13's schema checks cannot see it). Bad metadata must be skipped,
not fatal: the grader's three nonsense rows were. Good fits: per-customer custom fields, CMS content types (the
mini-project), admin tools. A poor fit: the core domain, which deserves reviewed SDL.
