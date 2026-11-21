# Lab 5.1 notes

## Timings (ms, median of 5 runs)

| Query | parse | validate | execute |
|---|---|---|---|
| `missing-club.graphql` (2 fields) | | | |
| 5,000 aliased `hello` fields | | | |

## Questions

1. Which phase grows fastest from 2 to 5,000 fields? Why would that be?
2. `run` refuses the 5,000-field query. Quote the error, name the limit, and say why the default exists.
3. In `phases`, how did you stop `execute` from parsing and validating again? What would be dangerous
   about using the same trick in a real server?
4. Compare `toSpecification()` for `syntax.graphql` and `missing-club.graphql`. When is `data` absent,
   when is it `null`, and when is it an object?
