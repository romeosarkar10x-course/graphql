# Hints, lab 3.8

1. `input BookLookup @oneOf { ... }`. graphql-java 26.1 knows the directive; you do not declare it. Every field of a oneOf input must be nullable and have no default (try `id: ID!` to see the startup error).
2. With `@oneOf`, `env.getArgument("by")` is a map with exactly one entry whose value is non-null. `by.entrySet().iterator().next()` and a `switch` on its key is all `find` needs.
3. Inhabited means "some finite value exists". `{a: {a: {a: ...}}}` never ends, and a oneOf must set exactly one field, so the chain cannot stop. Give the chain a way to stop.
