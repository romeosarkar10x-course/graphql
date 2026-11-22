# Lab 13.7: Code generation and dev tooling

Lecture: [13.7 Code generation and dev tooling](../../lectures/13.7-codegen-tooling.md). Grading: auto + rubric.
Needs Node.js (copied into `.cache/node` by `bin/course`, as in lab 13.6).

1. **Codegen in the build.** Add `graphqlcodegen-maven-plugin` 3.11.0 (DGS codegen for Maven; Netflix's own
   plugin is Gradle-only) to generate types and `DgsConstants` from the schema into `target/generated-sources`.
   Make every controller name its fields and arguments through `DgsConstants`
   (`@SchemaMapping(typeName = BOOK.TYPE_NAME, field = BOOK.AverageRating)`) and bind `createReview`'s input to
   the generated `CreateReviewInput`. Spring for GraphQL still serves: no DGS framework at run time. The
   experiment: the grader renames a field, an input field and an argument in the schema, and each rename must
   fail `mvn compile`. Behaviour must not change (Module 6's 14 checks).
2. **One config for every tool.** `graphql.config.yml` at the project root: where the schema is, where the
   operations are (`clients/`, the test documents), and a local endpoint. The grader loads it with graphql-config,
   the library behind graphql-eslint and the VS Code GraphQL extension; the IntelliJ GraphQL plugin reads the
   same file for completion and validation as you type.
3. **A docs site from the SDL.** `docs/spectaql.yml` for SpectaQL 3.0.9, built by `npm run docs` into
   `docs/public/index.html`. It must have a section for every type and every root field. For a picture of how the
   types connect, the reference server serves GraphQL Voyager at <http://localhost:8080/voyager.html> (Module 3).

```bash
bin/course start m13/07
cd workspace/m13-testing/07-codegen-tooling
bin/course shell -c 'mvn -q package'          # then look in target/generated-sources
bin/course node npm ci && bin/course node npm run docs
bin/course test m13/07
```

In `notes.md`: what the generated code buys (and what a rename does without it), what you chose not to generate,
one config for every tool, and docs from descriptions.

## Done when

`bin/course test m13/07` passes and `notes.md` passes the rubric (`bin/course grade m13/07`).
