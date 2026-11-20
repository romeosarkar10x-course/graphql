# Lab 2.6 — Introspection

**Lecture:** [2.6 Introspection](../../lectures/2.6-introspection.md) · **Grading:** auto + rubric · **Needs:** `bin/course ref up`; internet for part 3

The schema is queryable data. Read it by hand with `__type`, then turn a whole server's introspection
result back into SDL with graphql-java.

## Do

1. `bin/course start m02/06`, then work in `workspace/m02-query-language/06-introspection/`.
2. **By hand.** Finish `type-of.graphql` so it describes `Club`: every field's name, its arguments
   (`name`, `defaultValue`) and its type. `[User!]!` comes back as NON_NULL > LIST > NON_NULL > User, so
   you need three levels of `ofType`.
3. **With graphql-java.** Finish the three TODOs in `SchemaDump.java`:
   `IntrospectionQueryBuilder` builds the query, `IntrospectionResultToSchema` turns the result into an
   SDL document, and `SchemaPrinter` prints it.

   ```bash
   ../../../bin/course java SchemaDump.java http://shelfie-ref:8080/graphql
   ../../../bin/course java SchemaDump.java http://shelfie-ref:8080/graphql --count
   ```

4. **Public APIs.** Run `--count` against three public APIs from lab 0.3 (Countries, SWAPI, AniList,
   Rick and Morty...). Some will reject the standard query: retry with
   `IntrospectionQueryBuilder.Options.defaultOptions()` with `isOneOf`, `directiveIsRepeatable`,
   `inputValueDeprecation`, `specifiedByUrl` and `schemaDescription` set to false. Fill in `notes.md`.

## Done when

`bin/course test m02/06` is green (your dump must describe the same schema as the server's own
`/graphql/schema`) and `bin/course grade m02/06` gives 7/10 or more.
