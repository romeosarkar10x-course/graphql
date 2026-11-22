# Mini-project 17.9: build from scratch

Lectures 17.2 to 17.6. Grading: auto + rubric.

Your own engine: lexer, parser, the six validation rules, executor, introspection and printer, in **under 2,000
lines** of Java (Module 5's counter: blank, comment, package and import lines do not count). The starter has the
course's public API (`Token`, `Ast`, `Schema`, `Resolver`, `MiniGraphQL` and the signatures of the rest) with every
part stubbed, plus the fuzz test. You may change anything that is not a public signature the harness calls.

The test runs every check of labs 17.2 to 17.6 against your engine: the lexer and parser cases and differential
tests, the validation documents, the 30 conformance tests and the 12 round-trip checks. Then `DESIGN.md` (rubric).

## Solo

Write it all, in any order. Labs 17.2 to 17.6 are the same checks one part at a time, if you want to see one part
green before the next.

## Group

Two to four people, one engine. Split it along the API: lexer and parser; validation; execution; introspection and
printing. Agree on the AST records first (they are given) and on who owns `Schema`. Each person runs their own
part's checks with the others' parts taken from the course's reference engine (`contract/engine`), then you merge.
`DESIGN.md` has one section per person.

```bash
bin/course start m17/09
bin/course test m17/09
```

## Done when

`bin/course test m17/09` passes and `DESIGN.md` passes the rubric (`bin/course grade m17/09`).
