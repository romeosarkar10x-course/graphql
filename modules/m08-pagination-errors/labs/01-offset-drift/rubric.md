# Rubric, lab 8.1 notes (pass: 7 of 10)

| Points | Criterion |
|---|---|
| 2 | Q1 reports the drift numbers and explains them from the SQL: OFFSET counts positions, so an insert above the window pushes 3 already-seen rows into the next page (duplicates), and a delete pulls 3 unseen rows into the page already read (gaps). |
| 3 | Q2 has measured times for 4 pages at 1,000,000 rows and explains the growth (the database still reads and discards `page * size` rows). It also notices that `totalCount`'s `count(*)` is a fixed cost on every request, even page 0. Invented numbers score 0. |
| 3 | Q3 lists the statements one `books` request runs, and names a way to drop or bound the count (Spring Data `Slice`/`Window`, `hasNextPage` from `size + 1` rows, a cached or estimated total, counting only when `totalCount` is selected), with its cost to the client. |
| 2 | Q4 names a case where offset paging is the right choice anyway, with a reason (small or static lists, admin tables with page numbers, shallow pages, jump-to-page UIs). |
