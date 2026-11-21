# Mini-project 5 — A 150-line GraphQL server

**Lecture:** [5.4 Serving over HTTP yourself](../../lectures/5.4-http.md) (uses all of Module 5) · **Grading:** auto + rubric · **Needs:** nothing

Shelfie, served over HTTP by the JDK's `HttpServer` and graphql-java, with Jackson for JSON and no
other dependency. This is your Shelfie from now on: a Maven project (`mvn -q package`,
`java -jar target/shelfie-server.jar 4000`) with the reference seed in `Data.java` and the schema in
`src/main/resources/shelfie.graphqls`.

## The bar

| | |
|---|---|
| Size | at most **150 lines** of your code: `tests/count-lines.sh` skips blank, comment, `package` and `import` lines, and `Data.java` |
| Dependencies | graphql-java and jackson-databind (already in `pom.xml`); the test rejects others |
| Behaviour | **40 Hurl tests**: the 25-check GraphQL-over-HTTP audit from lab 5.4 and 15 Shelfie checks in `tests/shelfie.hurl` (every field of the schema, the viewer header, `createReview` and its `userErrors`, masked field errors) |
| Written | `DESIGN.md`: five decisions, pointing at your lines |

These 40 tests are `checkpoint-05`.

## Group variant

Pairs: one person writes the HTTP half (`serve` and its helpers), the other the wiring and
`createReview`, against the same 40 tests. Agree the seam (a method signature) in the first 10
minutes, then do not talk until both halves compile. Merge, run `bin/course test m05/09`, and write
`DESIGN.md` together.

## Done when

`bin/course test m05/09` is green and `bin/course grade m05/09` gives 7/10 or more.
