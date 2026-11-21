# Lab 5.6 — Five exceptions, five designs

**Lecture:** [5.6 Exceptions and errors](../../lectures/5.6-exceptions-errors.md) · **Grading:** auto + rubric · **Needs:** nothing

`Problems.java` (provided) holds Shelfie's domain exceptions and a `trouble(kind)` fetcher body that
throws one of them. `lab.graphqls` exposes it at the root and inside `Club`, once nullable and once as
`mustWork: String!`.

```
bin/course java Errors.java run ../tests/queries/five-kinds.graphql
```

The starter uses graphql-java's default handler. Read what it prints for `BUG`, then write a
`DataFetcherExceptionHandler`:

| Thrown | `extensions.classification` | Message | Other `extensions` |
|---|---|---|---|
| `NotFoundException` | `NOT_FOUND` | the exception's message | |
| `ForbiddenException` | `FORBIDDEN` | the exception's message | |
| `InvalidInputException` | `BAD_USER_INPUT` | the exception's message | `field` |
| `TimeoutException` | `UNAVAILABLE` | yours, without the internal service name | `retryable: true` |
| anything else | `INTERNAL` | yours, containing the `ref`, nothing internal | `ref`, also logged to stderr with the real exception |

Every error keeps its `path` and `locations`. Use your own `ErrorClassification` type for the
classifications.

## notes.md

Fill the design table (including whether each failure should be an error at all) and answer the four
questions.

## Done when

`bin/course test m05/06` is green and `bin/course grade m05/06` gives 7/10 or more.
