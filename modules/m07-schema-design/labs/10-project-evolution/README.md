# Mini-project 7 — The evolution simulator

**Lectures:** [7.6 Designing for evolution](../../lectures/7.6-evolution.md), with everything since 7.1 · **Grading:** auto + rubric · **Needs:** nothing

Shelfie release 0 is live: `v0.graphqls`, and the five operations in `clients/v0/` are installed on
phones that never update. Six product change requests are coming. Ship each one with **zero client
breakage**. You see one request at a time.

## Play

```
../../../bin/course java Sim.java
```

The first run reveals change request 1 (`requests/01.md`, plus its acceptance client in `clients/`).
Write the whole schema after that change as `steps/01.graphqls` (start from a copy of `v0.graphqls`)
and run `Sim.java` again. It checks the step and, when it passes, reveals the next request.

Every step must:

- keep every **deployed** client valid (release 0 until request 6 says otherwise),
- keep every earlier request's client valid,
- pass graphql-java's **SchemaDiff** against the previous step with nothing BREAKING,
- **deprecate** what it replaces, with a reason that names the replacement (the spec's default
  "No longer supported" doesn't count),
- stay valid with every deprecated element removed ([RFC #1231](https://rfcs.graphql.org/rfcs/1231/)).

Don't read `Sim.java`'s data string ahead of time: it only spoils the surprise, the tests don't care.

## Group variant (pairs or trios)

One player **ships** (writes each step), one plays **release 0** (owns `clients/v0/` and reviews each
step before `Sim.java` runs: are the deprecation reasons something a client developer can act on?), and
in a trio one plays **product** and reads each request aloud, answering questions only from its text.
Rotate roles every two requests. The step only counts if the reviewer approved it **and** `Sim.java`
passes; a step the simulator rejects after approval costs the reviewer a point too.

## Then write the ADR

`adr.md` is an Architecture Decision Record for your evolution policy: context, the rules you
followed, a row per step, consequences and the alternatives you rejected. It is the checkpoint's design
document.

## Done when

`bin/course test m07/10` is green (all six steps ship) and `bin/course grade m07/10` gives 7/10 or more.
