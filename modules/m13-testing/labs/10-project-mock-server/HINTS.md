# Hints: m13/10

1. Begin with your lab 13.4 `MockWiring` in a package and a `Main` that parses the options. Run MockCheck against
   `forge.graphqls` early: connections, custom scalars and interfaces of interfaces will show you which of your
   name rules and type rules were Shelfie-shaped.
2. Decide on `@mock` before execution: parse the document, find the operation (by `operationName`, or the only
   one), and look at its directives. Everything else is normal execution, which needs `@mock` declared in the
   schema to validate: merge the directive definition into the type registry before you build the schema.
3. The RFC's validity check is a second `GraphQL` over the same schema whose default data fetcher reads
   `env.getField().getResultKey()` from the parent map and whose type resolvers read `__typename`. Execute the
   operation with the mock's `data` as the root value: any error means the mock no longer fits.
