# Lab 17.3: Parser and AST, fuzzed

Lecture: [17.3 Parser and AST](../../lectures/17.3-parser.md). Grading: auto + rubric. Needs internet once (Maven
downloads Jazzer and graphql-java for the tests).

`Ast.java` holds the tree as records, and `Parser.parse(String)` throws. Write a recursive-descent parser for the
whole grammar (appendix C.4): operations (with the September 2025 descriptions on operations, fragments and
variables), fragments, selections, values, types, and the type system with its extensions. Each node with a `Loc`
gets the line and column of its first token. Use `switch` pattern matching over the sealed types wherever you walk
the tree.

The test runs:

- **21 fixed cases**, mostly where graphql-java 26.1 departs from the spec (empty `{}` in SDL, the fieldless-type
  lookahead, descriptions on operations, empty extensions);
- **a differential test** on the 1,000-document corpus: graphql-java's AST is converted to your records and compared
  with `equals()`, locations included (numbers by value: graphql-java keeps them as BigDecimal);
- **the fuzz test in regression mode**: `ParserFuzzTest` replays what Jazzer saved;
- **FUZZ.md** (rubric).

## Fuzz it

`src/test/java/dev/shelfie/mini/ParserFuzzTest.java` is a Jazzer fuzz test: your parser and graphql-java's on
inputs Jazzer makes up. A crash in yours is a finding and stops the run. A disagreement is saved to
`fuzz/disagreements/` and the run goes on. Known graphql-java departures are skipped.

```bash
bin/course shell
cd workspace/m17-under-the-hood/03-parser
JAZZER_FUZZ=1 mvn test -Dtest=ParserFuzzTest      # 60 s, one core, about 2 GB of memory
ls fuzz/disagreements | wc -l
```

Investigate every disagreement: group them by cause, cut each group down to a minimal example, decide who is right
from the spec text, fix your bugs, and fuzz again until no unknown disagreements are left. Then write `FUZZ.md`
(the starter has the headings). Commit any crash inputs Jazzer saves under `src/test/resources`: from then on they
are regression tests.

## Done when

`bin/course test m17/03` passes and `FUZZ.md` passes the rubric (`bin/course grade m17/03`).
