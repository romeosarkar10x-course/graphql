# Hints, lab 8.8

1. For NULL, don't print and re-parse: `AstPrinter` reformats the query, and every location moves.
   A `PreparsedDocumentProvider` gets the parsed, validated `Document` with the original source locations.
   Install one with `graphQL.transform(b -> b.preparsedDocumentProvider((input, parseAndValidate) -> ...))`,
   call `parseAndValidate.apply(input)`, and return a `new PreparsedDocumentEntry(newDocument)` unless
   the entry `hasErrors()`.
2. `OperationDefinition.transform(b -> b.directives(list))` and `Document.transform(b -> b.definitions(list))`
   build modified copies of the AST. A directive node is `Directive.newDirective().name("...").build()`.
3. For HALT, `ExecutionInput.cancel()` makes graphql-java stop before the next fetcher. Call it from the
   exception handler at the first error. One trap: `GraphQL.transform(b -> b.defaultDataFetcherExceptionHandler(h))`
   does nothing for a `GraphQL` that already has its strategies. Pass the handler in a new
   `AsyncExecutionStrategy(handler)` with `b.queryExecutionStrategy(...)`. Wrap
   `SimpleDataFetcherExceptionHandler` to keep the normal error shape.
