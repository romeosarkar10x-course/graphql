# Hints, mini-project 9

1. `BeforeTestExecutionCallback` and `AfterTestExecutionCallback` run immediately around the test
   method, after `@BeforeEach` and before `@AfterEach`. Keep the starting count in
   `context.getStore(namespace)`. Throwing an `AssertionError` from the after-callback fails the test.
2. Finding the budget: `AnnotationSupport.findAnnotation(method, MaxStatements.class)`, then the test
   class, then each enclosing class (`getEnclosingClass()`) for `@Nested`. If the test already failed,
   `context.getExecutionException()` is present: leave that failure alone.
3. For the message, group the recorded SQL by its text (collapse whitespace first) and sort by count.
   For CI: the `services:` block of a job runs `postgres:18.7-alpine` with a health check, and
   `mvn dependency:copy -Dartifact=org.postgresql:postgresql:42.7.14` fetches the driver for `ClubData.java`.
