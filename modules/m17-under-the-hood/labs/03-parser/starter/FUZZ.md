# Fuzz report

## The run

How long you fuzzed, how many inputs Jazzer tried (the `DONE` line), and the coverage it reached.

## Disagreements

Group the files in `fuzz/disagreements/` by cause. For each group: how many, one minimal example (as small as you
can cut it), what each parser did, and who is right. Quote the spec production or rule that decides it.

| Cause | Inputs | Minimal example | Yours | graphql-java | Spec says |
| --- | --- | --- | --- | --- | --- |

## Bugs you fixed

Each bug the fuzzer found in your parser: the input, what was wrong, the fix.

## What fuzzing did not find

One thing a fuzzer of this kind cannot tell you about your parser, and why.
