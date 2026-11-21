# Hints, mini-project 6

1. `Thread.currentThread().getContextClassLoader().getResources("dev/shelfie/app")` returns URLs; for
   `file:` walk the directory, for `jar:` cast `url.openConnection()` to `JarURLConnection` and list
   `getJarFile()` entries under the path. Skip names with `$` (nested classes).
2. `GraphQLCodeRegistry.newCodeRegistry().dataFetcher(FieldCoordinates.coordinates(type, field), fetcher)`;
   `RuntimeWiring.newRuntimeWiring().codeRegistry(registry)` hands it to the `SchemaGenerator`. `fetcher()`
   and `bind()` are written for you.
3. The `TypeDefinitionRegistry` from `SchemaParser` knows every type and its fields (`getType(name)`, and
   `objectTypeExtensions()` for `extend type`): check a coordinate there before registering it.
