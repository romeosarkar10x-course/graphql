# Checkpoint 13

`bin/course checkpoint 13` checks labs 13.1 to 13.7, game 13.8 (80% of PIT's mutants) and mini-project 13.10,
then tags `checkpoint-13`. Answer these first, without looking.

1. A controller drops an argument on its way to the service. Which kind of test notices: a unit test of the
   service, or a `@GraphQlTest` slice? Why?
2. Which Spring tester cannot see a bug in a `WebGraphQlInterceptor`?
3. Why is a subscription test that publishes right after subscribing flaky, and what is the fix?
4. Name a bug only a real PostgreSQL catches, and one only a real Kafka catches.
5. Your screen works on a schema-driven mock and crashes on the real server. What did the mock not produce?
6. A pull request adds `sortBy: String! = "title"` to `Query.books`. Breaking?
7. A layered ArchUnit rule allows graph → store. How do you still forbid controllers from calling repositories?
8. Why does a schema rename break the build with DGS codegen but not without it?

<details><summary>Answers</summary>

1. The slice: the service test calls the service directly and never runs the controller; the slice sends a
   document through the schema, the controller and the service.
2. `ExecutionGraphQlServiceTester`: it calls `ExecutionGraphQlService` directly, so no web layer and no
   interceptors run.
3. The feed is hot and the server registers the subscription after the client subscribes, so early events are
   lost. Wait for a signal that the server is listening (a subscriber count), and bound the test by time.
4. PostgreSQL: wrong `ORDER BY`, missing `ROUND`, a unique constraint. Kafka: the wrong topic name, a payload
   nobody can deserialise.
5. Nulls, empty lists and errors: a type-driven mock fills every field. Named mocks (MockSpec) give those states
   on purpose.
6. No: an argument is required only if it is non-null and has no default (Required Arguments); old queries
   validate and get "title". graphql-java's SchemaDiff still reports it as breaking.
7. A narrower rule: no class in `..graph..` may depend on a class annotated `@Repository`.
8. The controllers name fields through generated constants (`DgsConstants.BOOK.AverageRating`) that disappear
   with the schema field; without codegen the mapping is a method name, which the compiler cannot check against
   the schema, and Spring only logs the unmapped field.

</details>
