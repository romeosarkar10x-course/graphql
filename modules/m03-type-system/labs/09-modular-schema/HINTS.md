# Hints, lab 3.9

1. `extend type Query { clubs: [Club!]! }` in `clubs.graphqls` adds to the `Query` in the base file. An extension of a type that does not exist anywhere fails the merge ("missing its base underlying type"), which is why the base file sorts first (`00-base.graphqls`).
2. `@deprecated(reason: "...")` goes after the element: `size: Int! @deprecated(...)`, `titleContains: String @deprecated(...)` inside an argument list, `ON_HOLD @deprecated(...)`. graphql-java refuses to deprecate a non-null argument or input field without a default, because old clients could not stop sending it.
3. Coordinates have no spaces and no types: `Query.books(titleContains:)`, not `Query.books(titleContains: String)`. An argument coordinate's parent is a field; an input field is `Input.field` with no parentheses; directives start with `@`.
