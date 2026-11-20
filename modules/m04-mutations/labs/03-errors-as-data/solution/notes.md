# Lab 4.3 notes (reference)

## Lines of handling code

Reference: viaPayload 7 lines of mapping after the request, viaUnion 9 (switch with a nested block).
Roughly equal: the union pays for its `__typename` switch; the payload pays for interpreting codes.

## Which is easier to get wrong?

Payload: forgetting `userErrors` compiles, runs and returns `review: null`, read as success. Union: a
missing fragment returns only `__typename` for that member; the `switch` still sees the type name, and
the `default` branch fails loudly on unknown members. Selecting no fragment at all for a member yields
empty fields, which the record constructor exposes quickly. The union makes the outcome a *type*, so
the mistake is harder to make silently.

## Top-level errors

Request errors: a syntax or validation error in the document (wrong field name, wrong variable type,
`rating: "5"`), variable coercion failures. Execution errors: an unexpected exception in a resolver
(database down, a bug). Top-level errors are for the developer; `userErrors`/result members for the user.

## Question

Payload: a new `code` value `RATE_LIMITED`; old clients fall into their generic branch (show the
message), which degrades gracefully if they display `message`. Union: a new member type; old clients
that do not select it get `{__typename: "RateLimited"}` only and hit their unknown-type branch: must
exist (the switch's `default`), or they crash.
