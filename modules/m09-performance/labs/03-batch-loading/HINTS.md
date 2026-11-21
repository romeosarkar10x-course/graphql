# Hints, lab 9.3

1. `@BatchMapping Map<Club, Reader> owner(List<Club> clubs)` replaces the `@SchemaMapping` with the same
   name. `findAllById` on the distinct owner ids is one `in (...)` statement; build a map by id, then a
   map from each club to its owner. Keys are the club objects graphql-java passed in, so identity works.
2. Register in the controller's constructor:
   `registry.<MembersKey, List<Reader>>forName("members").registerMappedBatchLoader((keys, env) -> Mono.fromCallable(() -> ...))`.
   The `@SchemaMapping` method takes `DataLoader<MembersKey, List<Reader>> members`: Spring finds the
   loader by the parameter's name.
3. A load made in `thenCompose`, after graphql-java has dispatched the current level, is never
   dispatched unless chaining is on for the request: `DataLoaderDispatchingContextKeys.setEnableDataLoaderChaining(graphQLContext, true)`,
   from a `WebGraphQlInterceptor` using `request.configureExecutionInput(...)`.
