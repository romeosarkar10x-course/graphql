# Lab 2.3 — Aliases and multiple root fields

**Lecture:** [2.3 Aliases and multiple root fields](../../lectures/2.3-aliases.md) · **Grading:** auto + rubric · **Needs:** `bin/course ref up`

A comparison screen needs two clubs. Without aliases that is two requests or a custom endpoint; with
them it is one document.

## Do

1. `bin/course start m02/03`, then work in `workspace/m02-query-language/03-aliases/`.
2. `CompareClubs.java` is finished: it reads `data.left`, `data.right` and `currentBook.rating`. Change
   `compare-clubs.graphql` until `../../../bin/course java CompareClubs.java c1 c2` prints:

   ```
   club      The Midnight Readers               Slow Burn Mysteries
   members   8                                  4
   reading   The Left Hand of Darkness          The Name of the Rose
   rating    4.2                                4.5
   ```

3. Write `collision.graphql`: the same field twice, same response key, different arguments. Send it
   from GraphiQL and copy the error into `notes.md`.
4. Answer the other two questions in `notes.md`.

## Done when

`bin/course test m02/03` is green and `bin/course grade m02/03` gives 7/10 or more.
