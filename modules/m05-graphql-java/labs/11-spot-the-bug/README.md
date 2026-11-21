# Game 5b — Spot the bug

**Lecture:** [5.3 Async execution](../../lectures/5.3-async-execution.md) (uses 5.2 to 5.6) · **Grading:** auto + rubric · **Needs:** nothing

`Bugs.java` has eight fetchers marked `B1` to `B8`. Each one looks reasonable and survives a quick
manual query; each breaks in production: under load, with real data, or on another thread. The
mistakes are classics: blocking the common pool, shared mutable state, the wrong source type, mutating
what the store handed you, trusting an argument, a silent null in a non-null field, a `ThreadLocal`
across threads, and a pool that waits on itself.

```
bin/course java Bugs.java '{ club(id: "c1") { name owner { name } } }'
```

## Solo variant (timed)

Write the start time in `bugs.md`. For each bug: the symptom, the root cause, the fix, in the table.
Fix `Bugs.java` (keep `graphQL()`, `input()` and `RATING_THREADS`). `bin/course test m05/11` runs one
check per bug and prints your score. Stop the clock at 8/8. Par: 45 minutes.

Try to name each bug **before** running the test: the test tells you where, not why.

## Group variant

Round 1: everyone plays solo, fastest 8/8 wins. Round 2: each player plants two new bugs of their own
in a copy of the fixed file (one that a review would catch, one that only load would) and swaps with a
neighbour, who has 10 minutes to find both. A planted bug scores for the planter if it survives.

## Done when

`bin/course test m05/11` is green (8 of 8) and `bin/course grade m05/11` gives 7/10 or more.
