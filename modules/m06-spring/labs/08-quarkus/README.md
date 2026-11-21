# Lab 6.8 — Code-first Shelfie on Quarkus

**Lecture:** [6.8 Quarkus and MicroProfile GraphQL](../../lectures/6.8-quarkus.md) · **Grading:** auto + rubric · **Needs:** nothing

Quarkus 3.40 with `quarkus-smallrye-graphql` (MicroProfile GraphQL, implemented by SmallRye on
graphql-java). There is no schema file: SmallRye **generates** it from `@GraphQLApi` classes and serves
it at `/graphql/schema.graphql`.

1. Add `Club.owner`, `Club.currentBook` and `Book.averageRating` with `@Source` parameters.
   `currentBook` must be **batched**: one call per response, however many clubs (`currentBookBatches`
   counts the calls).
2. Make the generated schema match Shelfie where it matters: `club(id: ID!)`, `id: ID!`, non-null names,
   `owner: User!`, `authors: [String!]!`, and no `ownerId` leaking out.
3. Try dev mode (`mvn quarkus:dev`) and answer the notes.

`bin/course test m06/08` builds `target/quarkus-app/quarkus-run.jar`, starts it and runs `tests/lab.hurl`.

Not built: the plan's **SmallRye typesafe client**. It needs a second application to call this one;
Module 12 builds clients properly.

## Done when

`bin/course test m06/08` is green and `bin/course grade m06/08` gives 7/10 or more.
