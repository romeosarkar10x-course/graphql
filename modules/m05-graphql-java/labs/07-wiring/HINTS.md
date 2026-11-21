# Hints, lab 5.7

1. Diff `code` against `sdl` early and often; the printer shows every difference: a missing `!`, a
   description, a directive not registered with `additionalDirective`, a type only reachable through a
   `GraphQLTypeReference` (add it with `additionalType`).
2. `GraphQLCodeRegistry.newCodeRegistry().dataFetcher(FieldCoordinates.coordinates("Club", "owner"), df)`
   is what `RuntimeWiring` does for you. Fields without a registered fetcher get `PropertyDataFetcher`.
3. `SchemaTransformer.transformSchema(schema, visitor)` with a `GraphQLTypeVisitorStub` that overrides
   `visitGraphQLFieldDefinition` and returns `deleteNode(context)` for the fields to drop. For FastBuilder,
   pass the code registry builder and the root types to its constructor, then `addType` every type.
