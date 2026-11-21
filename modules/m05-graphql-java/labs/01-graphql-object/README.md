# Lab 5.1 — Three phases, one spec map

**Lecture:** [5.1 The GraphQL object](../../lectures/5.1-graphql-object.md) · **Grading:** auto + rubric · **Needs:** nothing (graphql-java runs in-process)

From this module on there is no framework: `Phases.java` builds a `GraphQL` object over Shelfie
(`shelfie.graphqls` + `Data.java`, wired for you in `Shelfie.java`) and talks to it directly.

```
bin/course java Phases.java run ../tests/queries/club.graphql '{"id":"c1"}'
```

(Run from your workspace copy; the queries live in this lab's `tests/queries/`.)

## Part 1: `run` prints the spec response map

The starter prints `{"data": ...}` and loses every error. Print what the spec says a response is: `data`,
`errors` and `extensions`, each present only when it should be. Five queries are checked, among them a
syntax error, a validation error and a missing variable.

## Part 2: `phases` times each phase

`phases <file>` parses with `graphql.parser.Parser`, validates with `graphql.validation.Validator`, then
executes **without parsing or validating again**, timing each step. Output:

```
parse 23.494 ms
validate 25.240 ms
execute 33.359 ms
validation-errors 0
fields 5000
```

`gen 5000` (provided) writes a query with 5,000 aliased fields. `run` must refuse it with graphql-java's
default limits; `phases` must get through it. Find out why and how.

## Part 3: notes.md

Fill the timing table (median of 5 runs, small and 5,000-field query) and answer the four questions.

## Done when

`bin/course test m05/01` is green and `bin/course grade m05/01` gives 7/10 or more.
