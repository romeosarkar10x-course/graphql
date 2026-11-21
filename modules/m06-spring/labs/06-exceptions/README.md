# Lab 6.6 — Six exceptions, one advice

**Lecture:** [6.6 Exception handling](../../lectures/6.6-exceptions.md) · **Grading:** auto + rubric · **Needs:** nothing

Lab 5.6 again, the Spring way. `Problems.java` (provided) throws six exceptions from `trouble(kind)`, at
the root and inside `Club`. Map them in a `@ControllerAdvice` with `@GraphQlExceptionHandler` methods:

| Thrown | `extensions.classification` | Message | Other `extensions` |
|---|---|---|---|
| `Problems.NotFound` | `NOT_FOUND` | its message | |
| `Problems.Forbidden` | `FORBIDDEN` | its message | |
| `Problems.Invalid` | `BAD_REQUEST` | its message | `field` |
| `Problems.Conflict` | any | its message | `code: "CONFLICT"`, `currentVersion` |
| `TimeoutException` | any | yours, without the service name | `retryable: true` |
| anything else | `INTERNAL_ERROR` | nothing internal | (Spring's default does this; keep it) |

Every error keeps its path and locations. Snapshot your six errors in `notes.md`.

## Done when

`bin/course test m06/06` is green and `bin/course grade m06/06` gives 7/10 or more.
