# Lab 5.7 notes

## Build time for 2,000 types (ms, third round)

| | sdl | builder | fast |
|---|---|---|---|
| 2,000 types | | | |

## Questions

1. Which way would you pick for Shelfie, and which for a tool that generates a schema from a database
   catalogue at startup? Give one reason each.
2. Where do fetchers live in each approach? Why does graphql-java keep them out of the type objects?
3. What does `GraphQLSchema.FastBuilder` skip to be that fast? What can go wrong if you use it with types
   you did not generate yourself?
4. Give one more real use of `SchemaTransformer` besides hiding `@internal` fields.
