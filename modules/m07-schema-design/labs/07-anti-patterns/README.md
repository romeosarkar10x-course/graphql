# Lab 7.7 — The hall of horrors, in-house

**Lecture:** [7.7 Anti-patterns and boundaries](../../lectures/7.7-anti-patterns.md) · **Grading:** auto + rubric · **Needs:** nothing

`bad.graphqls` (180 lines) is Shelfie's API after two years of "just expose the table": `getClub(club_id:)`,
`tbl_users`, `scalar JSON`, a 15-field `updateUser`, boolean-flag arguments, `created_at: Int`, a generic
`Response`, `deleteRow(table:)` and a `rawQuery(sql:)`. Seven client operations in `clients/` use it.

## Do

1. Lint the original to see the size of it:
   `../../../bin/course design lint bad.graphqls --rules naming,references,json-scalars,boolean-args,kitchen-sink,db-names,crud-names,timestamps`
2. `good.graphqls`: the same product, designed. It must pass all eight rules:

   | Rule | Flags |
   |---|---|
   | `naming` | anything not PascalCase / camelCase / UPPER_CASE |
   | `references` | output fields that are keys (`ownerId`, `member_ids`) |
   | `json-scalars` | `JSON`, `Any`, `Object`, `Map` scalars |
   | `boolean-args` | flag arguments (`include_past: Boolean`): use an enum or a separate field |
   | `kitchen-sink` | an `update*` mutation taking more than 5 fields |
   | `db-names` | `Dto`, `Entity`, `Row`, `tbl_`, snake_case columns |
   | `crud-names` | `get*`/`all*`/`find*` queries, `insert*`/`upsert*`/`patch*` mutations, raw SQL or table access |
   | `timestamps` | `*At`, `*_ts` fields typed `Int` or `String` |

3. Rewrite the seven clients in `clients/` (same file names, same purpose) against `good.graphqls`. No
   client may send its own user id: the server knows who is calling.
4. `notes.md`: the worst five, the split of `updateUser`, and the **bounded-context map**.

The lint is a floor, not the goal: a schema can pass all eight rules and still be bad. The rubric looks
at the design.

## Experiment

Map each Shelfie type to a bounded context (notes §3). Module 15 splits Shelfie into subgraphs along
exactly these lines, so mark the types that sit on a border.

## Done when

`bin/course test m07/07` is green and `bin/course grade m07/07` gives 7/10 or more.
