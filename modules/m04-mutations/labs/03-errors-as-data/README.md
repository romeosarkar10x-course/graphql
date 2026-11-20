# Lab 4.3 — Two error styles, one Java type

**Lecture:** [4.3 Errors as data](../../lectures/4.3-errors-as-data.md) · **Grading:** auto + rubric · **Needs:** `bin/course ref up`

The reference server has the same review rules behind two mutations:

- `createReview` returns `CreateReviewPayload { review userErrors { field message code } }`;
- `createReviewTyped` returns the union `CreateReviewResult = ReviewCreated | ValidationFailed | NotAllowed`.

`ReviewClient.java` maps both into one sealed Java type, `CreateReviewResult`, so the rest of the app
does not care which style the server used.

## Do

1. `viaPayload`: select `userErrors` and map: no errors → `ReviewCreated`; `UNAUTHENTICATED` →
   `NotAllowed`; anything else → `ValidationFailed` with every error's `field` as a dotted path
   (`input.rating`), in the server's order.
2. `viaUnion`: call `createReviewTyped`, select `__typename` and each member, map with a `switch` on
   `__typename` that **fails loudly on an unknown type**.
3. Run it. Both styles must print the same outcome for each of the four cases (ids differ: the two `ok`
   cases are different users).
4. `notes.md`: count the lines of handling code in each method, and answer the questions.

## Done when

`bin/course test m04/03` is green and `bin/course grade m04/03` gives 7/10 or more.
