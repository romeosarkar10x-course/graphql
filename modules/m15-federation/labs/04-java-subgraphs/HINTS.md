# Hints: m15/04

1. The Spring for GraphQL reference has a Federation page: one `GraphQlSourceBuilderCustomizer` bean that sets
   the schema factory, and one `FederationSchemaFactory` bean. After that, start-up names every entity type that
   has no `@EntityMapping` ("Unmapped entity types"), which is your to-do list.
2. The single form takes `@Argument String id` and is called once per representation. The List form takes all of
   them at once: `@Argument List<String> idList`, or `List<Map<String, Object>>` when you need each whole
   representation (two keys). The DataLoader form takes a `DataLoader<K, V>` argument, registered with
   `BatchLoaderRegistry`; a *mapped* batch loader leaves missing keys out and they load as null.
3. One DataLoader can serve several fields: register it by name, take it as a parameter with that name in each
   of the three `Book` field methods, and derive count and average from the same list. For unknown entities,
   Spring raises `RepresentationNotResolvedException`; a `@GraphQlExceptionHandler` decides what the router sees.
