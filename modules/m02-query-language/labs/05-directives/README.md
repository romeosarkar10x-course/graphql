# Lab 2.5 — Directives in operations

**Lecture:** [2.5 Directives in operations](../../lectures/2.5-directives.md) · **Grading:** auto + rubric · **Needs:** `bin/course ref up`

The club list shows a compact card; tapping it expands to a detailed card. Serve both from one
document and one Boolean variable.

## Do

1. `bin/course start m02/05`, then work in `workspace/m02-query-language/05-directives/`.
2. Change `club-card.graphql`:
   - add `$detailed: Boolean! = false`;
   - always return `id`, `name`, `memberCount`;
   - return `teaser` (the first member) **only on the compact card**, using `@skip`;
   - return `description`, `owner { name }`, `members(first: 3)` and `currentBook { title authors }`
     **only on the detailed card**, using `@include`. Group them in a fragment and put the directive
     on the spread.
3. In GraphiQL, compare `{"id": "c2"}` with `{"id": "c2", "detailed": true}`.
4. Write `subscription-skip.graphql` and run the experiments in `notes.md`. The reference server will not
   run subscriptions over plain HTTP, so validate with `../../../bin/course gql validate <file>`.

## Done when

`bin/course test m02/05` is green and `bin/course grade m02/05` gives 7/10 or more.
