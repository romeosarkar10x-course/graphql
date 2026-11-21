# Checkpoint 8 quiz

Answer without notes, then check.

1. Page 1 of an offset-paged feed starts with the last three books of page 0. What happened?
2. Why is `where (published, id) > (?, ?)` fast and `where published > ? or (published = ? and id > ?)` slow on PostgreSQL?
3. Name two things Spring for GraphQL 2.1's defaults get wrong for a public connection, and the fix for each.
4. Why must every sort end with a unique key?
5. A response has no `data` key. What kind of error is it, and should the client retry?
6. `members: [User!]!` and one member's `name: String!` fails. What does the client get for `club`?
7. What does an INTERNAL error tell the client, and how does support find the cause?
8. What changes for clients under `onError: NULL`?

<details><summary>Answers</summary>

1. Three books were added on top. OFFSET counts positions in the list as it is now, so the rows moved down.
2. The row value is one range on the `(published, id)` index: a seek. The OR of two ranges isn't, so
   the planner walks the index from the start and filters (900,000 rows removed at depth 900,000).
3. Cursors that contain Java class names: declare an `EncodingCursorStrategy<ScrollPosition>`.
   `first: -1` silently means the default: check the raw arguments. Also the OR-form keyset SQL: write
   row values in HQL.
4. Ties come back in any order, so rows move between pages and tests pass by luck (lab 8.4's mutation
   failed on try 2).
5. A request error (parse, validation, variables): nothing ran. Don't retry the same request; it's a
   client bug.
6. `club: null`. The null item can't live in `[User!]`, the list can't be null in `members: ...!`, so
   the nearest nullable position is `club`.
7. Only that it was the server's fault and that a retry may help: message `Internal error`, code
   `INTERNAL`. The `errorId` matches a log line with the full exception.
8. A `!` field can be `null` when there is an error at its path, and parents survive. Clients must check
   errors per path (or cache error-aware), and in return they get far more data and cacheable responses.
</details>
