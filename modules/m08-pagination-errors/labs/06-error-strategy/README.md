# Lab 8.6 — An error contract

**Lecture:** [8.5 Error strategy](../../lectures/8.5-error-strategy.md) · **Grading:** both · **Needs:** nothing

The Module 6 Shelfie app, plus three operations that fail on purpose (`myClubs`, `joinClub`,
`deleteReview`) and one that fails by accident (`Book.coverUrl`: object storage is down). The fetchers
already throw a `ShelfieException` with a `ShelfieError` code. Nothing turns those into a contract
yet: Spring answers every one of them with `INTERNAL_ERROR for <uuid>`. Build the two missing
pieces, then write the contract down for client developers.

Run the app from your copy with `bin/course shell -c 'mvn -q package -DskipTests && java -jar target/shelfie-errors.jar'`
(GraphiQL at <http://localhost:4000/graphiql> only if you publish the port. Use `curl` from a second
`bin/course shell` instead).

## Do

1. **`ErrorResolver`**, a `DataFetcherExceptionResolverAdapter` `@Component`:
   - `ShelfieException`: keep its message, set `errorType(code)` (a `ShelfieError` is an
     `ErrorClassification`), and add `extensions.code`.
   - Anything else: message `Internal error`, code `INTERNAL`, and an `extensions.errorId`. Log the
     exception with that id. The client must not see the original message.
2. **`ErrorPolicyInterceptor`**, a `WebGraphQlInterceptor` `@Component`, for what the resolver never
   sees:
   - Request errors (syntax, validation, variables) get `extensions.code: INVALID_REQUEST` too. Any
     error still without a code gets `INTERNAL`.
   - A request id goes into `GraphQLContext` before execution and into the response as
     `extensions.requestId`. The resolver uses it as the `errorId`. Take it from `X-Request-Id` when
     that is 1 to 64 letters, digits or `-`, and generate a UUID otherwise.
3. **`error-contract.md`**: fill in the template for a client developer, covering codes, retry rules,
   messages, partial data, request ids and how the contract may change.

## Experiment

Send the same five requests (no error, a `coverUrl` field error, signed-out `myClubs`, a validation
error, a syntax error) with `Accept: application/json` and with `Accept: application/graphql-response+json`.
Use `curl -s -o /dev/null -w '%{http_code}'`. Put the status codes in the contract's table and compare them
with GraphQL over HTTP, September 2026 edition (lecture 8.5 and Module 5's lab 5.4).

## Done when

`bin/course test m08/06` is green (10 Hurl checks, plus the `errorId` must lead to the logged exception),
and `bin/course grade m08/06` passes the contract.
