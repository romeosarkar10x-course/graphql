# Lab 13.7 notes

## 1. What the generated code buys

Every field, argument and type name in the controllers is a constant from `DgsConstants`, and
`createReview` binds the generated `CreateReviewInput`. Renaming `Book.averageRating` (R1), the input field
`body` (R2) or the argument `upcomingOnly` (R3) in the schema now fails `mvn compile` and points at the
controller line. Before codegen, the same renames compile and start: Spring's schema inspection logs
`Unmapped fields: {Book=[avgRating]}` at INFO, and the app answers `avgRating: null` (probed).

## 2. What I did not generate, and why

Output types stay the store's records. The generated `Book`, `Club` and `Review` classes would force a copy
from every record into a generated class on every fetch, and the payload's `review` would have to be the
generated `Review`. Generating inputs and names gets the compile-time safety where it is cheap; generating
outputs couples the domain to the schema's shape. DGS codegen also writes `UserError` with a builder,
which is used here because it has no domain counterpart.

## 3. One config for every tool

`graphql.config.yml` names the schema and the documents once. graphql-config (the same library behind
graphql-eslint and the VS Code extension; the IntelliJ GraphQL plugin reads the same file format) built 17
types from it and validated both client documents. The `endpoints` extension lets the IntelliJ plugin run
operations and re-introspect without another file.

## 4. Docs from the SDL

SpectaQL builds one 110 KB `index.html` from the schema file in about a second, with a section per type
and per root field, and the descriptions become the text. That is the real argument for lab 13.6's
`require-description`: the docs are only as good as the descriptions. Voyager (the reference server's
`/voyager.html`) answers a different question: how the types connect, not what each one means.
