# Hints: 16.1 observability

1. Spring for GraphQL already creates the observations; you only change what they are called and which key
   values they carry. Look for the two "convention" interfaces in `org.springframework.graphql.observation`:
   Boot uses a bean of either type instead of its default. Low-cardinality key values become metric labels and
   span attributes; high-cardinality ones only span attributes.
2. The client name must be in the GraphQL context before execution starts, so read the header in a
   `WebGraphQlInterceptor` (`request.configureExecutionInput(...)`). The request convention gets the
   `ExecutionInput` (and its `GraphQLContext`) from its context object; the data fetcher convention gets the
   `DataFetchingEnvironment`, whose parent type and field give you `Type.field`. Errors: the interceptor sees the
   response, `response.getErrors()`, each with its extensions.
3. Nothing in Prometheus? Check the three pieces separately: the dependency that adds `/actuator/prometheus`,
   the exposure property, and `percentiles-histogram` for `graphql.request` (without it there are no `_bucket`
   series). No traces? Spring Boot 4's OpenTelemetry starter, sampling probability 1.0, and the OTLP endpoint
   property under `management.opentelemetry.tracing.export.otlp`; tests read `OTLP_TRACES` from the environment.
