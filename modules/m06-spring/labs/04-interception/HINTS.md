# Hints, lab 6.4

1. `WebGraphQlRequest.configureExecutionInput((input, builder) -> builder.graphQLContext(Map.of(...)).build())`
   adds to the context; `chain.next(request)` returns a `Mono<WebGraphQlResponse>` you can `map` to set
   response headers and `transform` the result's extensions.
2. For the directive: visit the schema (`GraphQlSourceBuilderCustomizer` → `builder.typeVisitors(...)`
   with a `GraphQLTypeVisitorStub`), find fields with `hasAppliedDirective("uppercase")`, and replace their
   fetcher in the `GraphQLCodeRegistry.Builder` (`context.getVarFromParents(...)`) with a wrapper.
3. If your wrapper runs but nothing changes, print the class of the value it receives.
