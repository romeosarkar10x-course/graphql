# Hints: m14/07

1. Spring Boot builds `/graphql` around one `GraphQlSource` bean, and makes its own only when there is none.
   `GraphQlSource` is two methods, `graphQl()` and `schema()`: a bean of yours that returns whatever `GraphQL`
   instance is current replaces the schema file entirely.
2. Build the schema in code (`GraphQLObjectType.newObject()`, `GraphQLFieldDefinition`, a `GraphQLCodeRegistry`
   with one data fetcher per field reading its column from the row `Map`), then `GraphQL.newGraphQL(schema)`.
   Hold it in an `AtomicReference` and swap it whole; a `@Scheduled` check of a fingerprint of the metadata finds
   changes made by hand.
3. The admin endpoint is what Boot does for `/graphql`, by hand and not as a bean:
   `GraphQlSource.schemaResourceBuilder()`, `new DefaultExecutionGraphQlService(source)`,
   `WebGraphQlHandler.builder(service).build()`, `new GraphQlHttpHandler(handler)` (webmvc), and a
   `RouterFunction` for `POST /admin/graphql` that checks the key first.
