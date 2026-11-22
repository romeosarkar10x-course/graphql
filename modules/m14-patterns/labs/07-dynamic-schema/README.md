# Lab 14.7: Dynamic and multiple schemas

Lecture: [14.7 Dynamic and multiple schemas](../../lectures/14.7-dynamic-schemas.md). Grading: auto + rubric.
Needs PostgreSQL (`bin/course ref up`).

The starter serves `Book` from a fixed `.graphqls` file. A metadata table, `m14_07.fields`, already says which
columns of `m14_07.books` are fields and of which type (`STRING`, `INT`, `FLOAT`, `BOOLEAN`). Make it the source:

1. **The public schema (`/graphql`) is built at run time** from the metadata: `Book` has `id` and `title` and one
   field per metadata row, with its description; `books` and `book(id:)` read the table.
2. **Changes apply without a restart**: a row added by hand (`INSERT` plus `ALTER TABLE`) appears within seconds;
   requests in flight finish on the schema they started with. A row that makes no sense is skipped with a warning,
   never fatal.
3. **A second endpoint in the same JVM**, `/admin/graphql`, with its own schema (`admin/admin.graphqls`, provided):
   `fields`, `addField` (adds the column and the row, applies at once) and `removeField`. It requires the header
   `X-Admin-Key: course-admin-key` (401 without it). The public schema has no mutations.

```bash
bin/course start m14/07
cd workspace/m14-patterns/07-dynamic-schema
bin/course shell -c 'mvn -q package && java -jar target/shelfie-dynamic.jar'
curl -s localhost:4000/admin/graphql -H 'X-Admin-Key: course-admin-key' -H 'Content-Type: application/json' \
  -d '{"query":"mutation { addField(name: \"pageCount\", type: INT) { field { column } userErrors { message } } }"}'
bin/course test m14/07
```

The grader's experiment: a column added in the admin table appears as a field without a restart (same process
id), bad rows are skipped, and 150 queries during six schema swaps must all succeed.

In `notes.md`: a column added and a field appears, two endpoints in one JVM, when a schema should not be dynamic.

## Done when

`bin/course test m14/07` passes and `notes.md` passes the rubric (`bin/course grade m14/07`).
