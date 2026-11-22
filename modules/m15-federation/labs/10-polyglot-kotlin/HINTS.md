# Hints: m15/10

1. graphql-kotlin's federation module has its own generator: `toFederatedSchema` with a
   `FederatedSchemaGeneratorConfig` and `FederatedSchemaGeneratorHooks`, which take your entity resolvers. Keys and
   shareable fields are Kotlin annotations (`@KeyDirective`, `@ShareableDirective`).
2. An entity resolver implements `FederatedTypeSuspendResolver<User>` (or the promise form): a type name and a
   `resolve` from one representation map. The header reaches `me` through the `GraphQLContext` you build in
   `ExecutionInput`; a resolver takes `DataFetchingEnvironment` as a parameter.
3. If composition refuses your SDL with a federation version error, look at the `@link` your `_service` prints:
   the generator's default version may be newer than what the composer supports. The library lets you put your own
   `@LinkDirective` on a schema class passed as `schemaObject`.
