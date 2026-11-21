# Lab 8.3 — Connections in Spring for GraphQL

**Lecture:** [8.2 Cursor connections](../../lectures/8.2-cursor-connections.md) · **Grading:** auto · **Needs:** postgres (`bin/course ref up`)

Spring for GraphQL can generate the whole Relay connection for you: declare
`books(...): BookConnection!`, return a Spring Data `Window<Book>`, and `BookConnection`, `BookEdge`,
`PageInfo`, cursors and `pageInfo` appear. The starter does exactly that, and it works. It also leaks
Java class names in every cursor, pages with the slow SQL from lab 8.2, accepts `first: 100000`,
and treats `first: -1` as "use the default". Fix all four without giving up the generated types.

The app reads `m08_03.book` (10,000 books). The test loads it; to run the app yourself:
`bin/course catalog m08_03`, then `cd` into your copy and run
`bin/course shell -c 'mvn -q package -DskipTests && java -jar target/shelfie-connections.jar'`.
GraphiQL is not enabled; use `curl` or Hurl against `localhost:4000/graphql` from a second
`bin/course shell`.

## Do

1. **Opaque cursors.** Decode one of the starter's cursors (`echo <cursor> | base64 -d`). Replace Boot's
   cursor strategy so a cursor is `v1:published:id` under base64url and nothing else. Declare the bean
   **as `EncodingCursorStrategy<ScrollPosition>`**. A bean typed only as `CursorStrategy` gives Spring two
   candidates and quietly turns the `ScrollSubrange` argument off (lecture 8.2 explains why).
2. **Fast SQL.** Spring Data's `findBy(ScrollPosition, Limit, Sort)` writes the OR form. Write the two
   queries yourself in HQL with row values, `(b.published, b.id) > (:published, :id)`, fetch one extra row
   for `hasNextPage`, and build the result with `Window.from(content, positionFunction, hasNext)`.
   Backward pages are fetched descending and returned ascending.
3. **Rules for arguments**, each one an error with `extensions.classification: BAD_REQUEST` (use an
   `@GraphQlExceptionHandler`):
   - `first` or `last` above 100, or below 0. `ScrollSubrange` drops a negative count, so read the raw
     arguments too (`@Argument Integer first`, ...).
   - `first`/`after` mixed with `last`/`before`.
   - a cursor this server did not issue, base64 or not.
   - `first: 0` is legal: an empty page, no error.
4. Keep `BookConnection`, `BookEdge` and `PageInfo` out of the `.graphqls` file.

## Experiment

Load a million rows into the same schema (`bin/course catalog m08_03 1000000`) and page to the far end
with the starter and with your version. Use `last: 20` with a cursor from near the end, or `first: 20`
after one. Time both requests with `curl -w '%{time_total}'`, and copy the SQL from the log into
`EXPLAIN ANALYZE` (lab 8.2's notes). Then run `bin/course catalog m08_03` again to get the 10,000 rows back.
The test reloads them anyway.

## Done when

`bin/course test m08/03` is green: 14 Hurl checks, opaque cursors, row-value SQL, generated types.
