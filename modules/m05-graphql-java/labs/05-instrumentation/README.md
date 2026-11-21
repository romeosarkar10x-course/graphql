# Lab 5.5 — Which field is slow?

**Lecture:** [5.5 Instrumentation](../../lectures/5.5-instrumentation.md) · **Grading:** auto + rubric · **Needs:** nothing

`Latency.java` (provided, do not edit) is an `Instrumentation` that makes some Shelfie fields sleep, as
if they called slow backends. Which ones? Find out without reading it: write a timing instrumentation,
chain the two, and let the numbers tell you.

```
bin/course java Timing.java report ../tests/queries/club-pages.graphql
slow Book.averageRating 121 ms
slow ...
slowest Book.averageRating
fetches 68
```

| Mode | Output |
|---|---|
| `report <file>` | `slow <Type.field> <ms> ms` for every coordinate whose slowest **fetch** took over 50 ms, slowest first; then `slowest <Type.field>` and `fetches <n>` (every fetcher call, default ones included) |
| `run <file>` | the spec response map with `extensions.timing = {"fetches": n, "slowFields": [...]}` added by your instrumentation |

Rules: per-request state (`InstrumentationState`), not a static; `ChainedInstrumentation` to combine
yours with `Latency`; the numbers reach `main` through the result's extensions.

## notes.md

The slow-field table and four questions, one of which asks you to swap the hook you used and compare.

## Done when

`bin/course test m05/05` is green and `bin/course grade m05/05` gives 7/10 or more.
