# Hints, lab 2.6

1. `__type(name: "Club") { name kind fields { name } }` is a valid start. A field's `type` is a `__Type` too: it has `kind`, `name` and `ofType`. Named types have a name; wrappers (NON_NULL, LIST) have no name and an `ofType`.
2. `introspect(...)` returns the whole response as a `Map`; `IntrospectionResultToSchema().createSchemaDefinition(...)` wants only the `data` part. The `Document` it returns is SDL, not yet a schema: `new SchemaParser().buildRegistry(doc)` gives a registry.
3. `UnExecutableSchemaGenerator.makeUnExecutableSchema(registry)` builds a schema without resolvers. `new SchemaPrinter().print(schema)` prints it; `SchemaPrinter.Options` lets you leave out built-in directives. If a server answers with errors, check them before you read `data`.
