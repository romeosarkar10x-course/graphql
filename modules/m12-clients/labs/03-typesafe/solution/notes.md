# Type-safe clients: notes

Reference notes (grader only), from runs on 2026-11-22 (graphqlcodegen-maven-plugin 3.11.0, codegen
shared-core 8.8.0, Spring for GraphQL 2.1.0).

## What went over the wire

`GraphQLQueryRequest(ClubGraphQLQuery.newRequest().id("c1").build(), projection).serialize()` gives:

```graphql
{
  club(id: "c1") {
    name
    currentBook {
      title
      reviews(last: 2) {
        rating
      }
    }
  }
}
```

Arguments are **literals**, not variables, and the document has no operation name. Every club id is a
different document text: a server's parsed-document cache (graphql-java's `PreparsedDocumentProvider`,
lecture 9.8) misses for each id, APQ stores one hash per id, and a persisted-document allow-list would need one
entry per argument value. The generated `...WithVariableReferences` methods (e.g.
`reviewsWithVariableReferences("last")`) and the builder's variable references put `$variables` back.

## The rename

Book.title renamed to Book.name in the schema; `mvn compile`:

```
[ERROR] .../ClubPage.java:[33,27] cannot find symbol
[ERROR] .../ClubPage.java:[36,48] cannot find symbol
[ERROR] .../ClubPage.java:[49,60] cannot find symbol
```

(the projection's `.title()` twice and `Book.getTitle()`). Lab 12.2's digest keeps a document file and a
record: it compiles fine and fails at runtime, on the first request, with a `FieldAccessException`
(`FieldUndefined` validation error from Shelfie), and only if a test or a user calls that endpoint. Checking
documents against the schema in CI (Module 13) gets the same safety without generated code.

## The cost

46 classes in `client/` and 27 in `types/` for a 30-type schema, regenerated on every build. You give up a
readable document a reviewer, GraphiQL and the server's logs all share; the Java builder is harder to read
than the GraphQL it produces, and literal arguments hurt caching (above). Worth it when many services call
one big schema that changes often and you want breakage at compile time; not worth it for two documents.
