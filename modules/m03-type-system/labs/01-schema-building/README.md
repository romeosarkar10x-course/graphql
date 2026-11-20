# Lab 3.1 — Build a schema by hand

**Lecture:** [3.1 SDL and schema building](../../lectures/3.1-sdl-and-building.md) · **Grading:** auto + rubric · **Needs:** nothing (runs in-process)

No server, no framework: an SDL file, Java records and graphql-java 26.1. You build the same read
side the reference server has and run queries against it in-process.

## Files

| File | Yours? | What |
|---|---|---|
| `schema.graphqls` | read it | The contract. Do not change it. |
| `Data.java`, `Json.java` | no | Seed records (same values as the reference server) and JSON printing |
| `ShelfieSchema.java` | **yes** | The pipeline: `SchemaParser` → `TypeDefinitionRegistry` → `RuntimeWiring` → `SchemaGenerator` |
| `notes.md` | **yes** | The experiments |

Run it from the lab folder:

```bash
../../../bin/course java ShelfieSchema.java q.graphql '{"id":"c1"}'
../../../bin/course java ShelfieSchema.java --audit
```

## Do

1. **TODO 1, wiring.** The starter wires only `Query.club` and `Query.clubs`. Wire every field that
   does not match a record component: `Query.book`, `Club.owner`, `Club.members(first)`,
   `Club.memberCount`, `Club.currentBook`, `Book.averageRating`, `Book.reviews(last)`, `Review.author`.
   `Data.java` has the lookups.
2. **TODO 2, `--audit`.** Print every `Type.field` (for the types in `BACKING`) that has neither a
   registered data fetcher nor a record component of the same name, sorted, one per line. With the
   lab schema it prints nothing. With `--audit extra.graphqls` it also reads that file as an extension
   (the test adds `Club.bannerUrl` and `Book.subtitle`).
3. **Experiments** in `notes.md`.

## Done when

`bin/course test m03/01` is green and `bin/course grade m03/01` gives 7/10 or more.
