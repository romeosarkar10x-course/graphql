# Lab 13.4: Mocking

Lecture: [13.4 Mocking](../../lectures/13.4-mocking.md). Grading: auto + rubric. Needs the reference server.

Three kinds of fake, for three jobs.

**Part A: a schema-driven mock.** Write `MockWiring.java`, a graphql-java `WiringFactory` that makes up data for
**any** schema with no resolvers: a data fetcher for every field, a type resolver for every interface and union,
a scalar for every custom scalar. `MockServer.java` (provided, do not edit) serves it:

```bash
bin/course java MockServer.java /course/modules/m13-testing/contract/shelfie.graphqls 4000 13   # schema, port, seed
```

The grader checks responses with [contract/mock/MockCheck.java](../../contract/mock/MockCheck.java), on Shelfie's
schema and on one your mock has never seen (`tests/schemas/shop.graphqls`): exactly the selected fields, every
value of its type, non-null never null, enums declared, abstract types resolved to a possible type; the same
request twice gives the same response; another seed gives other data; and realistic values (emails with `@`,
URLs, ISO dates for `*At`, counts not negative, `id` arguments echoed, `first`/`last` sizing lists, no
placeholders such as `"String"`, no list where every item has the same name).

**Part B: a screen built on mocks.** Write `ReviewsScreen.java <bookId>`: it reads `SHELFIE_URL`, queries one book
and prints, exactly:

```
The Left Hand of Darkness by Ursula K. Le Guin (1969)
Rated 4.20, latest 3 reviews
- 4/5 @hugos: Estraven deserved better.
```

(`No ratings` when the average is null, `1 review` for one, the latest three reviews.) Build it against your mock
server only. The grader then runs the same file against the real Shelfie, with zero changes, for books `b1` and
`b5`.

**Part C: WireMock downstream.** `digest/` is a client that calls Shelfie for a user's reviews. Write
`digest/src/test` with WireMock (`@WireMockTest`) standing in for Shelfie: stub by operation name and variables,
cover an unknown user and an outage (errors with `data: null`). The grader plants five bugs in the client.

```bash
bin/course start m13/04
cd workspace/m13-testing/04-mocking
bin/course shell -c 'cd digest && mvn -q test'
bin/course test m13/04
```

In `notes.md`: what the mock never showed you, what path-seeded data gets right and wrong, and when WireMock is
the better fake.

## Done when

`bin/course test m13/04` passes and `notes.md` passes the rubric (`bin/course grade m13/04`).
