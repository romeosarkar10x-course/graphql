# Lab 13.2: GraphQL-aware tests

Lecture: [13.2 GraphQL-aware tests](../../lectures/13.2-graphql-tests.md). Grading: auto + rubric. Needs nothing.

Same Shelfie as lab 13.1, tested the way clients use it: documents in files, assertions on response paths and
on GraphQL errors, a subscription over a real WebSocket. Plus one DGS app (`dgs/`, lab 6.7's) tested with DGS's
own executor.

1. **Document files** in `src/test/resources/graphql-test/` (at least three), loaded with `documentName`.
2. **In-process** with `ExecutionGraphQlServiceTester` (`@SpringBootTest` + `@AutoConfigureGraphQlTester`): path
   assertions, and error assertions on classification, path and message (`review(id:)` for an unknown id is a
   `NOT_FOUND` error; deleting someone else's review is `FORBIDDEN`).
3. **Through the web layer** with `HttpGraphQlTester` (`@AutoConfigureHttpGraphQlTester`, MockMvc): the
   `X-Shelfie-User` header becomes the viewer only there.
4. **A subscription** with `WebSocketGraphQlTester` on a random port: subscribe to `reviewAdded(bookId: "b2")`,
   create reviews over HTTP (one for another book), and assert **exactly three events, in order**.
5. **DGS**: in `dgs/src/test`, a `@SpringBootTest` that runs documents with `DgsQueryExecutor`.

```bash
bin/course start m13/02
cd workspace/m13-testing/02-graphql-tests
bin/course shell -c 'mvn -q test && cd dgs && mvn -q test'
bin/course test m13/02
```

The grader plants seven bugs in the Spring app (a subscription that stops early, leaks other books, reorders; a
wrong error classification; an error without a path; a broken HTTP interceptor; userErrors without field paths)
and two in the DGS app, and runs your tests against each.

In `notes.md`: which tester saw which bug, how you made the subscription test reliable, path assertions versus
whole-response comparison, and `DgsQueryExecutor` against Spring's testers.

## Done when

`bin/course test m13/02` passes (all nine bugs caught) and `notes.md` passes the rubric (`bin/course grade m13/02`).
