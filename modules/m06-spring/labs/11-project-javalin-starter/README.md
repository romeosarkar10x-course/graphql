# Mini-project 6 — A tiny Javalin GraphQL starter

**Lecture:** [6.9 Framework bake-off](../../lectures/6.9-bake-off.md) (uses Modules 5 and 6) · **Grading:** auto + rubric · **Needs:** nothing

After a module of frameworks, write a small one. `dev.shelfie.tiny` is a library: annotations
(`@GraphQLController`, `@Field`, `@Arg`, `@Context`, given), a class-path scanner, and `TinyGraphQL`, which
loads a schema, wires annotated methods as fetchers and serves GraphQL over HTTP on Javalin 7 with lab 5.4's
`GraphQLHttp`. `dev.shelfie.app` (given) is Shelfie written against it, with no wiring code at all.

| TODO | Checked by |
|---|---|
| `Scanner.controllers(pkg)`: from directories **and** from the jar | the app runs as `java -jar target/tiny-graphql.jar` |
| `TinyGraphQL.build()`: register a fetcher per `@Field` method | lab 5.4's 25-check audit and the 14 Shelfie checks |
| Fail at startup on a method for a field not in the schema | the test renames a method to `memberCountt` and expects startup to fail naming it |

`DESIGN.md`: five questions about the API you just finished.

## Group variant

One person owns the library, the other the app. The app owner may only use the library's public API and
files issues against it; the library owner fixes them. Swap halfway.

## Done when

`bin/course test m06/11` is green and `bin/course grade m06/11` gives 7/10 or more.
