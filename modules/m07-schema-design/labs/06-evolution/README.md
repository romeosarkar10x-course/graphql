# Lab 7.6 — Change without versions

**Lecture:** [7.6 Designing for evolution](../../lectures/7.6-evolution.md) · **Grading:** auto + rubric · **Needs:** nothing (Docker on the host for graphql-inspector)

GraphQL APIs don't ship `/v2`. They change one additive step at a time, deprecate, measure, and remove
only what nobody uses. That works only if every change is classified before it ships, by a machine.

## Part A: classify 15 changes

`changes/base.graphqls` is the released schema; `changes/01.graphqls` to `15.graphqls` each make one
change (the first line says which).

1. In `classify.md`, fill **Guess** for all 15 **before running any tool**: BREAKING (some valid
   operation or response stops being valid), DANGEROUS (everything stays valid, but behaviour can
   change for clients), SAFE.
2. Run graphql-java's SchemaDiff on each: `../../../bin/course design diff changes/base.graphqls changes/01.graphqls`
   (the last line is the verdict). Fill **SchemaDiff**.
3. Run graphql-inspector 7.0.2 on each, on the host: `../../../bin/course inspector diff changes/base.graphqls changes/01.graphqls`
   (✖ breaking, ⚠ dangerous, ✔ safe). Fill **inspector**. The tools don't always agree.

## Part B: a gate in the build

`schema-gate/` is a Maven project: `released/schema.graphqls` is what clients use today,
`src/main/resources/schema.graphqls` is the next release.

1. Ship three changes in `src/main/resources/schema.graphqls`, with no breaking change:
   - `Club.memberCount: Int`
   - `CreateReviewInput.spoiler: Boolean`
   - replace `Club.description` with `Club.about: String` (Markdown). Clients still use `description`:
     keep it, deprecated with a reason that names the replacement.
2. `SchemaCompatibilityTest`: two JUnit tests.
   - `noBreakingChangesAgainstTheReleasedSchema`: SchemaDiff between the two files, fail on any BREAKING
     event with its message, print the DANGEROUS ones.
   - `stillValidWithDeprecatedElementsRemoved`: [RFC #1231](https://rfcs.graphql.org/rfcs/1231/)'s
     rule. Drop every `@deprecated` field and enum value and check the rest still builds.
   Run it: `cd schema-gate && ../../../../bin/course shell -c 'mvn -q test'`.
3. `.github/workflows/schema.yml`: on pull requests, run `mvn -B test` in `schema-gate` and graphql-inspector
   (`npx --yes @graphql-inspector/cli@7.0.2 diff <released> <current>`). The test checks the file, it
   doesn't run GitHub Actions.

The test also puts changes 02, 04, 05 and 09 and a deprecated-only schema into a copy of your gate and
checks it fails or passes as it should.

## Done when

`bin/course test m07/06` is green and `bin/course grade m07/06` gives 7/10 or more.
