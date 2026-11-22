# Hints: m13/01

1. Test the edges, not the middle: a rating of 5 passing says nothing about 6. For every rule write the last
   value that is allowed and the first that is not, and for the body try spaces only and 500 characters with
   spaces around them.
2. A unit test that calls `CatalogService.books("the", 3)` cannot notice a controller that drops `titleContains`
   on the way: the bug is between the schema and the service. Only a test that sends a GraphQL document goes
   through that code. The same goes for a controller that invents a viewer.
3. `@GraphQlTest` scans `@Controller` and `@ControllerAdvice` classes but not `@Service` or `@Repository`: give it
   real services over the real seed in a `@TestConfiguration`. The slice has no `ViewerInterceptor`, so set
   `GraphQLContext`'s "viewer" yourself with the tester's `configureExecutionInput`; and do not declare a second
   `Clock` bean, the application class already has one.
