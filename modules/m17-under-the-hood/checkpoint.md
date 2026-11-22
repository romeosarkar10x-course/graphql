# Checkpoint 17

`bin/course checkpoint 17` checks lab 17.3 (parser and fuzzing), lab 17.8 (the HTTP audit) and mini-project 17.9
(your engine, every suite), and that `FUZZ.md`, `AUDIT.md` and `DESIGN.md` are in your workspace, then tags
`checkpoint-17`. The written parts are graded separately (`bin/course grade m17/NN`). Answer these first, without
looking.

1. Name the section of the spec that decides each: an alias collision; a null in a non-null field; `3000000000` as
   an `Int`.
2. Why must a lexer count columns in code points, and what does a lone CR do to the next token's position?
3. `type T { u @s }`: what does the spec say, what does graphql-java 26.1 do, and which grammar notation decides?
4. Your differential fuzzer reports 2,000 disagreements in a minute. What do you do with them?
5. Why does the lab compare only "fires or not" for NoFragmentCycles and OverlappingFieldsCanBeMerged?
6. What is the difference between a request error and an execution error, in the response?
7. Why may query fields run in parallel but mutation root fields not, and how does the response keep its order?
8. graphql-http warns that your server returns 422 to a legacy client's invalid document. Why do you keep it?

<details><summary>Answers</summary>

1. 5.3.2 Field Selection Merging; 6.4.4 Handling Execution Errors (propagation to the nearest nullable parent);
   3.5.1 Int (outside -2^31 to 2^31 - 1).
2. The spec's source text is Unicode code points and errors locate them; UTF-16 counts some as two. A lone CR is a
   line terminator: the next token is on the next line, column 1 (graphql-java 26.1 keeps it on the same line).
3. A syntax error: the fieldless object type has `[lookahead != {]`, so braces after the header must be fields.
   graphql-java reads `type T` and then a shorthand query `{ u @s }`.
4. Group them by cause, shrink one example per group, decide each from the spec text, fix your bugs, mark the
   other implementation's known departures, and fuzz again until nothing unexplained is left.
5. The spec defines when a document is invalid, not how many errors to report; graphql-java and graphql-js count
   those two differently.
6. A request error (parse, validation, operation, variables) gives `errors` and no `data` entry; an execution error
   makes its position null (propagating through non-null types) and appears in `errors` with `path` and
   `locations`, next to `data`.
7. Mutation root fields have side effects, so 6.2.2 says to execute them serially; query fields are free of side
   effects and may run "normally". The executor joins the futures in document order.
8. The September 2026 edition says to handle a legacy client's request as if it accepted
   `application/graphql-response+json` and to change the Content-Type only on 2xx; validation failures are 422.
   graphql-http 1.23.1 predates the edition.

</details>
