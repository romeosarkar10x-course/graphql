# Lab 13.1: Unit tests

Lecture: [13.1 Unit tests](../../lectures/13.1-unit-tests.md). Grading: auto + rubric. Needs nothing.

The starter is Shelfie under test ([contract/shelfie-app](../../contract/shelfie-app/)): controllers in `graph/`
only translate GraphQL, the rules live in `service/` (`ReviewService`, `CatalogService`), data in `store/`. Write
two kinds of test:

1. **Unit tests for the rules**, under `src/test/java/dev/shelfie/app/service/`: JUnit and Mockito, no Spring
   context. `ReviewService`'s javadoc lists its rules (sign-in, book exists, rating 1 to 5, trimmed body of 1 to
   500 characters, one review per user per book, every broken rule reported), plus the average's rounding, the
   order of `reviewsOf`, who may delete, and the live feed.
2. **A `@GraphQlTest` slice** for the controllers: schema, controllers and `ErrorAdvice`, no web server. Test what
   the controllers pass through: arguments and their schema defaults, the viewer from `GraphQLContext`, errors.

```bash
bin/course start m13/01
cd workspace/m13-testing/01-unit-tests
bin/course shell -c 'mvn -q test'      # your tests, fast feedback
bin/course test m13/01                 # the grader
```

The grader runs your `src/test` against the contract app's `src/main` (yours is ignored), first unchanged
(every test must pass), then with **eight planted bugs**, one at a time. Each bug must make at least one of your
tests fail; the output names the tests that caught it. One of them deletes a resolver's logic (the syllabus
experiment): note which of your tests noticed.

In `notes.md`: which tests noticed the deleted resolver logic and why, where each kind of test belongs, and what
mocking the repositories cost.

## Done when

`bin/course test m13/01` passes (all eight bugs caught, service tests without Spring, a `@GraphQlTest` slice) and
`notes.md` passes the rubric (`bin/course grade m13/01`).
