# Hints, lab 8.6

1. Override `resolveToSingleError(Throwable ex, DataFetchingEnvironment env)` and return
   `GraphqlErrorBuilder.newError(env).errorType(code).message(...).extensions(Map.of("code", ...)).build()`.
   `newError(env)` fills in the path and location for you. Return `null` only for exceptions you want
   another resolver to handle. Here you want them all.
2. In the interceptor, `request.configureExecutionInput((input, b) -> b.graphQLContext(c -> c.put("requestId", id)).build())`
   runs before execution, and the resolver reads it back with `env.getGraphQlContext().get(...)`. After
   execution, `chain.next(request).map(response -> response.transform(b -> ...))` lets you replace the
   errors (`b.errors(list)`) and the extensions (`b.extensions(map)`). Read the originals from
   `response.getExecutionResult()`.
3. A `GraphQLError` is immutable. Rebuild it with `GraphqlErrorBuilder.newError().message(e.getMessage())
   .locations(e.getLocations()).path(e.getPath()).errorType(e.getErrorType()).extensions(newMap)`.
   Request errors have `getErrorType()` values like `ValidationError` and `InvalidSyntax`. Keep the
   ones that already have a `code`. The `@Order` on the interceptor matters only if you have several.
