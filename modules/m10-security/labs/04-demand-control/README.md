# Lab 10.4: Demand control

Lecture: [10.4 Demand control](../../lectures/10.4-demand-control.md). Grading: auto + rubric. Needs nothing.

The starter is lab 10.2's solution, with the authorization rules in place and no limits at all: every
attack from lab 10.3 that is about demand still works on it. Add five limits, then tune them until the
attacks fail and Shelfie's real screens still pass.

| # | Limit | How |
|---|---|---|
| 1 | depth 10, 500 fields | graphql-java 26's `QueryComplexityLimits`, put in each request's `GraphQLContext` (defaults: depth 100, 100,000 fields) |
| 2 | cost 5,000 | `MaxQueryComplexityInstrumentation` with your `FieldComplexityCalculator`; lists multiply by `first` |
| 3 | `first` between 0 and 50 | refuse with `BAD_REQUEST` |
| 4 | 1 second per request | cancel the `ExecutionInput` |
| 5 | 10,000 cost points per caller per minute | a Bucket4j token bucket (already in the pom); refusals carry `extensions.code: RATE_LIMITED` and `retryAfterSeconds` |

Every response reports `extensions.cost: {requested, remaining, limit}`. `Book.recommendations` is slow on
purpose (300 ms a call) and should weigh 10.

```bash
bin/course start m10/04
bin/course test m10/04
bin/course java /course/modules/m10-security/contract/Shape.java query.graphql   # depth and fields of any document
```

## Done when

`bin/course test m10/04` passes: the 7 real screens (`tests/real.hurl`), the 7 refused attacks
(`tests/attacks.hurl`), root's fifth 2,110-point query in a minute is `RATE_LIMITED` while ada is
unaffected, and `notes.md` passes the rubric (`bin/course grade m10/04`).
