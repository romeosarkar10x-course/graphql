# MicroProfile clients: notes

Reference notes (grader only), from runs on 2026-11-22 (Quarkus platform 3.40.1.2).

## The same call, twice

Typesafe: a 4-line interface (`Club club(@NonNull @Id String id, @NestedParameter("currentBook.reviews")
@Name("last") int last)`) plus the records; the records *are* the selection set. Dynamic: a 7-line nested
builder (`document(operation(QUERY, "clubCard", vars(...), field("club", args(...), field("name"), ...)))`)
plus `response.getObject(Club.class, "club")`. The typesafe one is easier to write and to call; the dynamic
one is easier to review and debug, because the document is visible in the code, and it can build selections
at runtime (a field picker, an admin tool). For a 2 a.m. failure the dynamic version wins: what you read is
what was sent.

## What each one sent

```
typesafe: {"query":"query club($id: ID!, $last: Int!) { club(id: $id) {name memberCount owner {name} currentBook {title authors publishedYear reviews(last: $last) {rating body author {name}}}} }","variables":{"id":"c1","last":2},"operationName":"club"}
dynamic:  {"query":"query clubCard($id:ID!){club(id:$id){name memberCount owner{name} currentBook{title authors publishedYear reviews(last:2){rating body author{name}}}}}","variables":{"id":"c1"}}
```

The typesafe client chose the selection set (every record component, recursively), the variable types (from
Java types and annotations) and the operation name (the method name). First attempt, with `@Id String id`
and no `@NonNull`: Shelfie refused it, `Variable 'id' of type 'ID' used in position expecting type 'ID!'`
(VariableTypeMismatch). The dynamic client sends no `operationName` even for a named operation.

## Typesafe here, generated in 12.3, document files in 12.2

- 12.2: the `.graphql` file is the truth; the record just has to match. A rename fails at runtime.
- 12.3: the schema is the truth; codegen turns it into projection classes. A rename fails at compile time.
- 12.4 typesafe: the Java records are the truth; the document is derived from them. A rename fails at runtime
  (a validation error) unless the record changes too; no schema is involved at build time.
- 12.4 dynamic: the Java builder is the truth, like a document file written in Java.
