# Hints: m13/04

1. Try the starter first: graphql-java's `MockedWiringFactory` makes any schema executable and answers null
   everywhere, so a nullable root is null and a non-null one is an error. Your factory says `true` to
   `providesDataFetcher`, `providesTypeResolver` (interfaces and unions) and `providesScalar` (custom scalars
   only), and returns one fetcher for everything.
2. Make every value a function of the seed and `env.getExecutionStepInfo().getPath()`: deterministic, different
   per position, no state. Choose by the field's type (unwrap non-null and lists), then by its name. Return a
   `Map` for objects; put an echoed `id` argument in it, and let the child fetcher read it back.
3. A type resolver gets the object your parent fetcher returned: keep the path in that map, so it can pick a
   possible type by hash. For the screen, the mock never returns null; ask what the real Shelfie returns for a
   book nobody has reviewed.
