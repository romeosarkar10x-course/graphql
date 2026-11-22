# Hints: m14/01

1. `RuntimeWiring.Builder.directive("mask", wiring)` calls your `onField` once for every field that carries
   `@mask`, while the schema is built. `env.getAppliedDirective().getArgument("keep").getValue()` is the value
   written in the SDL (or its default). Throwing there stops the start-up, which is what misuse should do.
2. Wrap, do not replace: get the current fetcher with `env.getCodeRegistry().getDataFetcher(coordinates, field)`,
   register a new one that calls it and transforms the value. Then two directives on one field wrap each other,
   and the order they are written decides who runs first.
3. A transformation that fails at run time should return `DataFetcherResult` with an error built from the
   `DataFetchingEnvironment`; a thrown exception becomes Spring's opaque `INTERNAL_ERROR`. And the next directive
   in the stack may receive that `DataFetcherResult` instead of a value.
