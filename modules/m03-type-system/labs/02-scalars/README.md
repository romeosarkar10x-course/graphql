# Lab 3.2 — Long IDs, safely

**Lecture:** [3.2 Built-in scalars, Java-side](../../lectures/3.2-scalars.md) · **Grading:** auto + rubric · **Needs:** nothing

Shelfie's analytics store keys rows with Java `long`s, and one key is `9007199254740993` (2^53 + 1).
Page views passed three billion. The starter maps both to `Int`, as a first draft would.

## Do

1. Run the starter: `../../../bin/course java Scalars.java q.graphql` with
   `{ review(id: 9007199254740993) { id } }` and `{ stats { totalPageViews } }`. Read both errors.
2. Fix `schema.graphqls` and `Scalars.java`:
   - `AnalyticsReview.id` and the `review(id:)` argument become `ID!`. IDs travel as JSON strings, so
     no client rounds them. Parse incoming IDs safely: a malformed id returns `null`, not an error.
   - `Stats.totalPageViews` becomes `Long!` from graphql-java-extended-scalars
     (`ExtendedScalars.GraphQLLong`). Declare `scalar Long` with `@specifiedBy(url: ...)`.
   - A client sending the id as a JSON number (`{"id": 9007199254740993}`) still gets the review.
3. Experiments in `notes.md`.

## Done when

`bin/course test m03/02` is green and `bin/course grade m03/02` gives 7/10 or more.
