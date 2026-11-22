# Lab 13.1 notes

## 1. Which tests noticed when a resolver's logic was deleted (mutant U5)?

Only the slice test `ShelfieControllerSliceTest.booksPassesTheFilterAndTheCapThrough`. The service tests call
`CatalogService.books("the", 3)` directly, so they never go through `QueryController`; when the controller
drops `titleContains`, every service test still passes. The slice test sends a GraphQL document, so the
argument has to travel through the schema, the controller and the service. That is the whole case for
having both: service tests pin the rules, slice tests pin the plumbing.

## 2. Unit test or slice test: where does each of your tests belong?

Rules with many cases (rating bounds, trimming, every broken rule reported, rounding) are unit tests:
fast (the whole class runs in well under a second), no Spring, and the parameterised bounds cost one line
each. Anything that depends on the schema (defaults such as `first: Int = 20`, argument names, the viewer
from GraphQLContext, error classification from `ErrorAdvice`) needs the slice, because none of that exists
outside graphql-java's execution. A slice test that re-checks every rating case would be slow and would
fail for the wrong reason (a schema typo) as often as for the right one.

## 3. What did mocking the repositories cost?

The mocks know the repository's API (`all`, `add`, `find`), so a refactor of the store breaks the service
tests even when behaviour is unchanged. `CatalogServiceTest` uses the real `CatalogRepository` instead,
because it is plain data and cheaper than a mock. Mock what is slow or has side effects; use the real
thing when it is a value.
