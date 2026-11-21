# Hints, lab 10.2

1. Count the doors to a private review: `node(id: "v1")`, `club(id: "c1") { reviews }`,
   `books { reviews }`, `node(id: "r4") { clubs { reviews } }`. Each needs the same check, so write it once
   (`canSee(Club)`, `canSee(Review)`) and call it from every resolver that returns a club or a review.
2. `@PreAuthorize("@access.isMember(#clubId)")` calls a bean named `access` with the method's argument.
   Spring for GraphQL turns the resulting `AccessDeniedException` into `FORBIDDEN`, or `UNAUTHORIZED` when the
   caller is anonymous.
3. In `SchemaDirectiveWiring.onField`, read the role from `env.getAppliedDirective()`, get the existing
   fetcher from `env.getCodeRegistry()` by `FieldCoordinates`, and register a wrapper that checks the role
   before calling it.
