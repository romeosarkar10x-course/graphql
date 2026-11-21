# Hints, lab 6.6

1. Run the test's query against the starter first: Spring already masks everything. Your job is to
   un-mask the five you mean.
2. A `@GraphQlExceptionHandler` method takes the exception (its type selects the handler) and may take
   the `DataFetchingEnvironment`; it returns a `GraphQLError` (or a list).
3. `GraphqlErrorBuilder.newError(env).errorType(ErrorType.NOT_FOUND).message(...).extensions(Map.of(...))`
   keeps path and locations from the environment.
