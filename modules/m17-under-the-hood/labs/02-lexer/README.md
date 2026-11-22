# Lab 17.2: Lexer

Lecture: [17.2 Lexer](../../lectures/17.2-lexer.md). Grading: auto.

The starter is the course's small engine, `minigql`, with everything but the lexer: `Token.java` declares the
tokens as a sealed interface of records (`Punct`, `Name`, `IntValue`, `FloatValue`, `StringValue`, `End`), and
`Lexer.lex(String)` throws. Write it from the spec's lexical grammar (sections 2.1 and 2.10, appendix C.1 to C.3):

- skip what the spec ignores: the byte order mark, white space, line terminators, comments and commas;
- lex greedily, and reject a number followed by a digit, a `.` or a name start (`0x1`, `1.`, `00`);
- resolve escapes, including variable-width `\u{1F600}` (RFC #849) and surrogate pairs written as two `\u` escapes;
  a lone surrogate is an error;
- give block strings the value `BlockStringValue()` (2.10.4) computes;
- give each token its line and column, both from 1, columns in code points;
- throw `SyntaxError` with a line and column for anything the grammar does not allow.

The test compiles the course harness against your engine and runs:

- **20 fixed cases** (`contract/harness/.../Cases.java`), several where graphql-java 26.1 departs from the
  September 2025 spec: there your lexer must follow the spec;
- **a differential test**: your tokens against graphql-java's ANTLR lexer on 1,000 documents (300 of the course's
  own `.graphql` files and 700 generated with seed 17). Tokens are compared by kind, value, line and column.
  Documents where graphql-java is known to differ from the spec are counted as "known", not against you.

```bash
bin/course start m17/02
bin/course test m17/02
# one document at a time, inside the image:
bin/course shell
source tools/testlib.sh; source modules/m17-under-the-hood/contract/harness.sh
harness_build /course/workspace/m17-under-the-hood/02-lexer && harness TokenDiff 1000 17 --show 30
harness Corpus 1000 17 /tmp/corpus     # the corpus as files, to read the document a disagreement names
```

## Experiment

Run the differential test with the known cases counted (comment out the `Known.lexer` line in a copy of
`TokenDiff`): which graphql-java behaviours does it find, and what does the spec say for each?

## Done when

`bin/course test m17/02` passes.
