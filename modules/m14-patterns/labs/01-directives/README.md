# Lab 14.1: Custom schema directives

Lecture: [14.1 Custom schema directives](../../lectures/14.1-directives.md). Grading: auto + rubric. Needs nothing.

The schema (`src/main/resources/graphql/shelfie.graphqls`) declares three directives and uses them; its
descriptions are the exact rules. Resolvers return raw values (EUR amounts, whole emails). Make the directives
true with one `SchemaDirectiveWiring` each, registered through a `RuntimeWiringConfigurer`:

- `@uppercase`: String and `[String]` fields, `Locale.ROOT` (ß becomes SS).
- `@mask(keep: Int = 4)`: every character but the last `keep` becomes `*`.
- `@currency(code: String!)`: an EUR amount converted at Shelfie's fixed rates and written `"USD 13.50"`, rounded
  half even to the currency's own decimals (yen has none).

Misuse must stop the server **at start-up**, naming the field: `@uppercase` on a Float, an unknown currency code,
a negative `keep`. A value a directive cannot handle at run time is a field error, not a 500: the rest of the
response survives.

The experiment: `Experiment` has two fields with the same two directives in opposite orders. Does order matter?

```bash
bin/course start m14/01
cd workspace/m14-patterns/01-directives
bin/course shell -c 'mvn -q package && java -jar target/shelfie-directives.jar'
bin/course test m14/01
```

The grader runs `directives.hurl` (13 checks), then your jar with **its own copy of the schema with the directives
moved to other fields** (behaviour must follow the SDL, not field names), then three broken schemas that must fail
start-up.

In `notes.md`: the stacking result and why, directive or code, and where a directive is the wrong tool.

## Done when

`bin/course test m14/01` passes and `notes.md` passes the rubric (`bin/course grade m14/01`).
