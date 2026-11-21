# Lab 6.8 notes

## Dev mode

Measured (`_workbench/2026-11-21-m05-probe/quarkus-dev-mode.txt`): after the edit, the next request
triggered "Restarting quarkus due to changes in ShelfieApi.class ... Live reload total time: 0.423s", and
`clubs(first: Int)` was in `/graphql/schema.graphql` 2.5 s after saving (most of it the test's polling and
compile). No build, no restart, no schema file to edit: in
code-first the Java signature is the schema.

## Questions

1. Java cannot say non-null without an annotation (an object field may always be null), and SmallRye
   cannot tell an id from any other String, so it picks the safe, loose reading: nullable `String`.
   Clients notice: generated client types are all optional, and they must null-check `name` forever. A
   schema reviewer notices only if someone reads the generated schema; nothing forces it.
2. Three calls for three clubs, and N calls (N queries, N round trips) for N clubs: the N+1 problem. The
   batched version receives all parent clubs at once and can issue one `WHERE id IN (...)` query. In
   Spring the same comes from `@BatchMapping`, built on java-dataloader (Module 8).
3. Public API: schema-first. The schema is the contract that clients and reviewers read, and its
   evolution is deliberate (Module 13), not a side effect of refactoring Java. An internal service with
   one team: code-first is fine, even pleasant: one source of truth, no drift, dev mode feedback.
4. Quarkus started in 0.32 s against Spring's 0.8 s on the build machine; Quarkus does its classpath
   scanning and schema generation at build time (the `build` goal) and starts with less reflection,
   which also lowers RSS. Lab 6.9 measures it properly.
