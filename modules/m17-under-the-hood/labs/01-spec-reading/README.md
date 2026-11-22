# Lab 17.1: Reading the spec

Lecture: [17.1 Reading the spec](../../lectures/17.1-spec.md). Grading: auto + rubric.

Five things you saw servers do in earlier modules. For each, find the section of the **September 2025 edition**
(https://spec.graphql.org/September2025/) that decides it, put the section number in `answers.json`, and in
`NOTES.md` quote the sentence or algorithm step and say which step a server that got it wrong skipped.

| Id | Behaviour |
| --- | --- |
| `subscription-skip` | `subscription { reviewAdded @skip(if: $x) { id } }` is rejected before anything runs (Module 11) |
| `two-operations` | a document with two operations and no `operationName` gets no data at all (Module 2) |
| `alias-collision` | `{ book(id: 1) { name: title name: description } }` is invalid (Module 2) |
| `null-propagation` | an error in a non-null field nulls its parent object (Module 7) |
| `int-range` | `3000000000` is not an `Int` (Module 3) |

Answers are section numbers as the spec prints them (`6.4.4`, not "Handling Execution Errors"). The test names the
ones that are not right yet and never says what they should be. Then answer the three RFC-process questions at the
end of `NOTES.md` from https://github.com/graphql/graphql-spec/blob/main/CONTRIBUTING.md and
https://rfcs.graphql.org.

```bash
bin/course start m17/01
bin/course test m17/01
bin/course grade m17/01      # the rubric, for NOTES.md
```

## Done when

All five sections are right and `NOTES.md` passes the rubric.
