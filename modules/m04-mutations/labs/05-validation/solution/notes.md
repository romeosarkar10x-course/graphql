# Lab 4.5 notes (reference)

## The table

type: 03, 04, 05, 09, 16, 17, 20 (7). directive: 01, 02, 06, 08, 10, 13, 14, 18, 19 (9). bean: 07, 15
(2). business: 11, 12 (2). Most rules are shape rules and belong in the schema.

## Two surprises

04: `4.5` for an `Int` is rejected by variable coercion, before any directive; whole floats like `5.0`
would pass. 07: twelve spaces pass `@Size(min: 10)`; only `@NotBlank` sees they are empty. 13 vs 14:
`@Size` on a list checks each element, `@ContainerSize` checks the list. 19: a literal out of range is
still caught by the directive (at execution), not by validation, because `@Range` is not part of
GraphQL validation.

## Error shapes

Case 01: a top-level error, `message: "/createReview/input/rating range must be between 1 and 5"`,
`extensions.classification: {type: ExtendedValidationError, validatedPath: [createReview, input, rating],
constraint: @Range}`, and `data: null` (the `CreateReviewPayload!` is non-null, so the null reaches the
root: lab 3.4). Case 07: `userErrors: [{field: ["input","body"],
message: "must not be blank", code: "INVALID"}]`. The bean error is per-field data a form maps directly;
the directive error needs the client to read `extensions.validatedPath`. To unify, translate extended
validation errors into `userErrors` (a DataFetcherExceptionHandler or a wrapping fetcher), or accept
two shapes and document both.

## Question

The rules are invisible to clients at runtime: a client team must read the SDL file (or a schema registry)
to duplicate them, and they drift. Options: publish the SDL with directives (graphql-java can print them),
or document the limits in descriptions, which introspection does expose.
