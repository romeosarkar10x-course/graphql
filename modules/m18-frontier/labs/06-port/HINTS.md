# Hints: 18.6 Porting a subgraph to another language

1. Get the signatures right first: run the test and read the diff it prints. Pothos makes every field nullable
   unless told otherwise, and the contract is mostly non-null; one builder option flips the default for the
   whole schema.
2. Federation in Pothos: `builder.asEntity(ref, { key, resolveReference })` for the type reviews owns (`Review`),
   `builder.externalRef('Book', builder.selection<{ id: string }>('id'), resolve)` for the ones it extends, and
   `UserRef.provides<{ handle: string }>('handle')` on `Review.author`, with `handle` as an external field of
   `User`. `toSubGraphSchema` needs a `linkUrl` for Federation v2.9, which the course's composer knows.
3. If the plugin fails to load with an import error from `@apollo/subgraph`, read the plugin's own
   `devDependencies` and pin the version it was tested against: the newest `@apollo/subgraph` is not always the
   one that works. For `addReview`, the error must match Java's: a `GraphQLError` with
   `extensions.classification: "UNAUTHORIZED"`, and Yoga must not mask it.
