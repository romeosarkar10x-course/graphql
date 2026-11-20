# Lab 1.3 — One GraphQL call

**Lecture:** [1.3 The mental model](../../lectures/1.3-mental-model.md) · **Grading:** auto + rubric · **Needs:** `bin/course ref up`

Same page, same [contract](../../contract/club-page.md), one request. The HTTP code is done for you
(you wrote it in lab 0.4); you write the query and the mapping.

## Do

1. `bin/course start m01/03`. Write `club-page.graphql` in GraphiQL first (http://localhost:8080/graphiql,
   variables `{"id": "c1"}`), then paste it into the file.
2. Finish the mapping in `GraphQlClubPage.java`. Run it from the lab folder:
   `../../../bin/course java GraphQlClubPage.java c1`.
3. Repeat lab 1.2's latency experiment for this program and fill in `notes.md`, including the REST row.

Look at the arguments in the docs pane: the schema already has `members(first:)`, `reviews(last:)` and
`meetups(upcomingOnly:)`. Let the server cut the lists.

## Done when

`bin/course test m01/03` is green and `bin/course grade m01/03` gives 7/10+.
