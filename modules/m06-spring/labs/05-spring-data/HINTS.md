# Hints, lab 6.5

1. `@GraphQlRepository` on the repository interface, and the repository must also be a
   `QueryByExampleExecutor<Book>` (or `QuerydslPredicateExecutor`) for Spring to generate fetchers.
2. Spring matches a repository to Query fields by return type: the GraphQL type `Book` and the entity's
   simple name `Book`. Arguments are bound by name onto an example entity.
3. Spring Data derives SQL from method names: `findBy...ContainingIgnoreCase...OrderBy...`.
