# Fuzz report (reference solution)

## The run

Six runs of `JAZZER_FUZZ=1 mvn test -Dtest=ParserFuzzTest`, 60 s each, one core. The last: 488,901 inputs,
coverage 5,544 edges and 25,306 features, corpus 1,648 inputs (260 KB), no disagreements left that the course does
not know about. The first run wrote 2,625 disagreeing inputs to `fuzz/disagreements/`.

## Disagreements

| Cause | Inputs (run 1) | Minimal example | Ours | graphql-java | Spec says |
| --- | --- | --- | --- | --- | --- |
| Lone CR as a line break | most of the 2,233 "trees differ" | `{s\ru}` | `u` at 2:1 | `u` at 1:3 | LineTerminator is LF, CRLF or CR (2.1.2): a lone CR starts a line. graphql-java's ANTLR lexer counts LF only. |
| Descriptions on operations | 252 + 150 (and 33 rejected) | `"" subscription{s}` | accepts, description `""` | rejects | `OperationDefinition : Description? OperationType ...` (2.3, September 2025, RFC #1170). graphql-java 26.1 predates it. |
| Empty braces in SDL | 202 | `type r {}` | rejects | accepts, no fields | `FieldsDefinition : { FieldDefinition list }`: a list has at least one item. graphql-java's grammar says `fieldDefinition*`. |
| Braces that are not fields | (run 4) | `type z {u @s}` | rejects | `type z` and the query `{u @s}` | The fieldless form has `[lookahead != {]`: after `type z`, a `{` must start fields. ANTLR backtracks instead. |
| Block strings with CR or short blank lines | 77 + 19 + 7 | `"""a\r\nb"""` | `a\nb` | `a\r\nb` | BlockStringValue() splits on every line terminator and joins with LF, and removes the indent from every line (2.10.4). |
| `extend scalar` with nothing | 21 | `extend scalar S` | accepted | rejects | **Our bug**: `ScalarTypeExtension : extend scalar Name Directives[Const]` needs the directives. |

U+2028 and U+2029 (graphql-java ignores them as line terminators; the spec does not list them) did not come up in
these runs; the differential corpus and the fixed cases cover them.

## Bugs fixed

- `extend scalar S`, `extend type T` and the other extensions were accepted with nothing in them. Every extension
  production requires at least one of its optional parts. The parser now checks what each extension adds.
- Not a parser bug, but found the same way: run 3 stopped with a stack overflow. It was the harness's own regular
  expression for block strings (Java's regex engine recurses on alternations), on a 2 KB input. Replaced by
  graphql-java's raw tokens.

## What fuzzing did not find

Agreement is not correctness: where both parsers are wrong in the same way, a differential fuzzer stays quiet.
And random bytes rarely make long valid documents, so deep, well-formed nesting (and the parser's stack depth on
it) is tested by the corpus and by hand, not by the fuzzer.
