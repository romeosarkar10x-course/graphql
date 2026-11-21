# Hints, lab 6.3

1. The four argument errors are primitives (`int`, `boolean`) bound to nullable schema arguments. A
   wrapper type plus a fallback fixes each; try `{ books(first: null) { id } }` against the starter first.
2. `GraphQlSourceBuilderCustomizer` is in `org.springframework.boot.graphql.autoconfigure`; its builder has
   `inspectSchemaMappings(Consumer<SchemaReport>)`. Throwing from the consumer stops the context.
3. Without `@NullMarked`, unannotated methods have unknown nullness and the inspector cannot report the
   third mismatch. `org.jspecify.annotations` is already on the class path; for nested types write
   `Data.@Nullable Club`.
