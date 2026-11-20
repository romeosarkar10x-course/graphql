# Lab 2.8 — Documents as data

**Lecture:** [2.8 Documents as data](../../lectures/2.8-documents-as-data.md) · **Grading:** auto + rubric · **Needs:** nothing (no server)

Write a tiny analyzer: for any document, report each operation's depth, field count and alias count.
It is the seed of the cost limits in Module 10.

## The metrics

Counted after replacing every fragment spread with the fragment's selections:

| Metric | Means | `{ club(id: "c1") { owner { name } } }` |
|---|---|---|
| `depth` | Longest chain of nested fields; a root field alone is 1 | 3 |
| `fields` | Every field node, `__typename` included, each time it appears | 3 |
| `aliases` | Every field node with an alias | 0 |

Output: one JSON line per operation, `{"operation":"Name","type":"query","depth":3,"fields":3,"aliases":0}`
(`"operation":null` when anonymous). A fragment cycle or an unknown fragment prints a message to stderr
and exits with status 1. `--print` prints each operation with fragments expanded, using `AstPrinter`.

## Do

1. `bin/course start m02/08`, then work in `workspace/m02-query-language/08-analyzer/`. Parsing and
   output are written; you finish `expand` and `measure`.
2. Try it on the fixtures:
   `../../../bin/course java Analyzer.java ../../../modules/m02-query-language/labs/08-analyzer/tests/fixtures/04-compare-clubs.graphql`.
3. Answer the questions in `notes.md`.

## Done when

`bin/course test m02/08` is green and `bin/course grade m02/08` gives 7/10 or more.
