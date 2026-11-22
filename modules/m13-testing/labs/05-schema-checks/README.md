# Lab 13.5: Schema checks in CI

Lecture: [13.5 Schema checks in CI](../../lectures/13.5-schema-checks.md). Grading: auto + rubric. Needs nothing.

Write `SchemaCheck.java`, the schema gate a pull request must pass:

```bash
bin/course java SchemaCheck.java <old.graphqls> <new.graphqls> <clients-dir>
```

It diffs the committed schema against the new one with graphql-java's `SchemaDiff`, finds which known client
operations each change touches (`clients-dir`, all `*.graphql` files, any depth), validates every operation
against the new schema, and prints, in this format:

```
BREAKING Book.isbn: The new API is missing the field 'Book.isbn'
  why: an operation that selects or sends it fails validation, before any resolver runs
  used by BookPage (clients/web/BookPage.graphql)
DANGEROUS Visibility.UNLISTED: ...
  unused by every known client operation
INVALID BookPage (clients/web/BookPage.graphql): Validation error (FieldUndefined@[book/isbn]) ... at clients/web/BookPage.graphql:5:5
verdict: FAIL (1 breaking and used, 0 breaking but unused, 0 dangerous, 1 invalid operations of 5)
```

Coordinates are `Type.field`, `Input.field`, `Enum.VALUE`, and `Type.field(arg:)` for a changed default. Exit 1
when a breaking change touches an operation that uses it, or any operation no longer validates; a breaking change
no known client uses is reported and allowed (a registry check's rule). A changed default only touches operations
that leave the argument out.

The grader runs your check on 11 scenarios against `tests/clients/` (removed and retyped fields, a required
argument and a required input field, enum values added and removed, a changed default, a deprecation, a stale
client) and expects the right exit code and lines.

Then the pipeline: `.github/workflows/schema.yml`, on pull requests, with two jobs: graphql-inspector's action
(`graphql-hive/graphql-inspector@...`) to annotate the diff, and a job that runs `SchemaCheck` and explains a
failure in the job summary. The course cannot run GitHub Actions; run graphql-inspector locally to compare:
`bin/course inspector diff old.graphqls new.graphqls`.

In `notes.md`: the pull request that breaks a client, a breaking change nobody uses, where graphql-inspector and
SchemaDiff disagree, and why the workflow has both checks.

## Done when

`bin/course test m13/05` passes and `notes.md` passes the rubric (`bin/course grade m13/05`).
