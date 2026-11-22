# Mini-project 12.9: Build your own normalised cache

Lecture: [12.5 Normalised caching](../../lectures/12.5-normalised-caching.md). Grading: auto. Needs nothing.

Take your lab 12.5 `NormalizedCache.java` and pass all 25 scenarios in
[contract/cache/scenarios/](../../contract/cache/scenarios/), the full [SPEC.md](../../contract/cache/SPEC.md):

| | Scenario | What a real client calls it |
|---|---|---|
| 01–10 | lab 12.5's core | normalisation |
| 11–13 | named and inline fragments, `possibleTypes`, `@include`/`@skip` | fragment matching |
| 14 | `keyFields`: an entity identified by `isbn` | `keyFields` (Apollo Client), `@typePolicy(keyFields:)` (Apollo Kotlin) |
| 15–16 | `keyArgs`, and pages appended under one key | field policies, `offsetLimitPagination`, connection handling |
| 17 | a list's book is a hit for `book(id:)` | cache redirects, `toReference` |
| 18–19 | a mutation updates entities, not lists; writing the list back | `update` functions, `cache.modify` |
| 20–22 | evict an entity, evict a field, garbage-collect | `cache.evict`, `cache.gc` |
| 23–24 | optimistic layers, and the server's answer under them | `optimisticResponse` |
| 25 | watches fire on their own changes only | `watchQuery`, `ApolloStore.watch` |

```bash
bin/course start m12/09
cd workspace/m12-clients/09-project-cache
cp ../05-normalized-cache/NormalizedCache.java .   # then add the starter's extra methods
bin/course test m12/09
```

## Group variant: scenario swap

Everyone builds their own cache. Then each player writes **two new scenarios** in the same JSON format (SPEC.md)
that follow the spec but that they think other caches get wrong: deep fragments, a variable inside an input
object, a list of lists, an optimistic write of a new entity, a watch through a redirect. A scenario counts only
if it passes the reference behaviour described in SPEC.md (argue it out as a group, or have an AI grader read
SPEC.md and rule).

Put everyone's scenarios in `scenarios/` next to your cache (numbered `50-` and up) and run `bin/course test
m12/09`: it prints how many of the group's scenarios your cache passes. One point for every other player's cache
your scenario breaks; one point for every scenario of theirs your cache passes. Fix, rerun, highest total wins.

## Done when

`bin/course test m12/09` passes all 25 scenarios.
