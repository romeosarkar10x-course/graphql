# Lab 6.3 — Make the schema and the code agree

**Lecture:** [6.3 Schema mapping inspection](../../lectures/6.3-inspection.md) · **Grading:** auto + rubric · **Needs:** nothing

At startup Spring for GraphQL compares the schema with your controllers and logs a "GraphQL schema
inspection" report: unmapped fields, mappings with no field, arguments, and (new in 2.0, using
JSpecify) nullness. The starter (lab 6.1's app) has four errors in it. Nobody reads logs.

1. Read the starter's report and paste it into `notes.md`. Make it clean.
2. Annotate nullness: `@NullMarked` on the package, `@Nullable` where the schema allows null.
3. Make any mismatch **fail startup**: a `GraphQlSourceBuilderCustomizer` bean that inspects the report.

The test then injects three mismatches into a copy of your app (a schema field nobody maps, a renamed
controller method, `currentBook: Book!`) and expects startup to fail naming all three.

## Done when

`bin/course test m06/03` is green and `bin/course grade m06/03` gives 7/10 or more.
