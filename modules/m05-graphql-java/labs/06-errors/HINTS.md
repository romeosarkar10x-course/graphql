# Hints, lab 5.6

1. `GraphQL.newGraphQL(schema).defaultDataFetcherExceptionHandler(handler)` replaces the default for
   queries and mutations. The handler gets `DataFetcherExceptionHandlerParameters`: the exception, the
   path and the `DataFetchingEnvironment`.
2. `GraphqlErrorBuilder.newError(env)` copies path and locations from the environment. `errorType(...)`
   takes any `ErrorClassification`; an `enum` implementing that interface is the usual way.
3. A `switch` over the exception with type patterns (`case Problems.NotFoundException nf ->`) keeps the
   mapping in one place; the `default` branch is where bugs land, so that is where you mask and log.
