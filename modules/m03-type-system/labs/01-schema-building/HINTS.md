# Hints, lab 3.1

1. `RuntimeWiring.newRuntimeWiring().type("Club", t -> t.dataFetcher("owner", env -> ...))`. In a fetcher, `env.getSource()` is the parent object (here a `Data.Club`), and `env.getArgument("first")` is the argument after defaults are applied.
2. Why do `id` and `name` work without wiring? graphql-java's default data fetcher looks for a getter, a record accessor or a map key with the field's name. `ownerId` is not `owner`, so `owner` gets nothing.
3. For the audit: `schema.getCodeRegistry().hasDataFetcher(FieldCoordinates.coordinates(type, field))` tells you if a fetcher was registered; `Class.getRecordComponents()` tells you what the default fetcher could find. To read an extension file, parse both files and `merge` the registries before generating.
