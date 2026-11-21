# Hints, lab 6.2

1. A `@SchemaMapping` method may return `graphql.execution.DataFetcherResult`. Spring passes its
   `localContext` to the children, and `@LocalContextValue(name = "club")` looks the name up in it, which
   only works if the local context is a `GraphQLContext`.
2. Two methods mapped to the same coordinate is a startup error. If `LabController` takes over
   `Club.owner` and `Club.members`, remove them from `ShelfieController`.
3. `ArgumentValue<String>` can be a component of the input record. `isOmitted()` is true when the key
   was not sent at all; otherwise `value()` is what was sent, possibly `null`.
