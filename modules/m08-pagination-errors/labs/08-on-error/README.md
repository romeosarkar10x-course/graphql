# Lab 8.8 — onError: NULL, PROPAGATE, HALT

**Lecture:** [8.7 Where error handling is heading](../../lectures/8.7-error-handling-future.md) · **Grading:** both · **Needs:** nothing

RFC #1163 (RFC 1, Proposal) lets a request choose what happens when a field errors: `PROPAGATE`
(today: the null travels up to the nearest nullable parent), `NULL` (the null stays where the error
happened) or `HALT` (stop, `data: null`, one error). No Java server reads `onError` yet. You build a
prototype on graphql-java 26.1, then measure what each mode leaves a client cache.

`Page.java` (don't edit; the test uses its own copy) is a club page where `Book.rating` always fails and
user u3's `name` fails. Under PROPAGATE that is `data: null`. Run `bin/course java OnError.java` to
see all modes.

## Do

1. **NULL** in `OnError.execute`. graphql-java has the behaviour behind an experimental directive,
   `@experimental_disableErrorPropagation`, on the operation. Add it to the **parsed document**, not
   to the query text: error `locations` must still point into the client's own query (line 1, column 51
   for the rating error). Request errors (syntax, validation) stay request errors.
2. **HALT**: at the first field error, stop. No later fetcher may start (the test counts at most 3), and
   the response is `data: null` with exactly that one error.
3. **`NormalizedCache.write`**: make it error-aware. A field whose path is in `errors` is not
   written, so an outage never overwrites a cached value with `null`. Expected: PROPAGATE stores 0
   fields, NULL stores 14 in 6 records, HALT stores 0.

## Done when

`bin/course test m08/08` is green (12 checks) and `bin/course grade m08/08` passes the notes.
