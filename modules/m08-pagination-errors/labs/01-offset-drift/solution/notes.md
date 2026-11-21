# Lab 8.1 notes

## Q1. Drift: what did Drift.java print for insert and for delete? Why exactly those numbers?

```
insert: pages 5, size 20, shown 100, distinct 88, duplicates 12, gaps 0
delete: pages 5, size 20, shown 100, distinct 100, duplicates 0, gaps 12
```

`OFFSET 20` means "skip the first 20 rows as the list is now", not "continue after the book I saw
last". Three inserts at the top push every row down three places, so the last 3 books of page 0 are
now at positions 20 to 22 and come back as the first 3 of page 1. That happens after each of the
first 4 pages: 4 x 3 = 12 duplicates. Three deletes among rows already read pull every later row up
three places, so 3 rows the reader never saw slide into the window that was already read: 12 gaps. A
UI shows the duplicates (a React list with the same key twice). Nobody notices the gaps, which makes
them worse.

## Q2. Deep pages at 1,000,000 rows

| Page (size 20) | Time per request (curl, several runs) |
|---|---|
| 0 | 19 to 24 ms (first request 191 ms, a cold JVM) |
| 500 | 19 to 20 ms |
| 5,000 | 24 to 26 ms |
| 49,999 | 101 to 104 ms |

(Reference run, 2026-11-21, `_workbench/2026-11-21-m08-probe/offset-1m.txt`.) Why it grows: PostgreSQL
walks the `(added_at, id)` index backwards and throws away `page * 20` rows before returning 20. At
the last page that is 999,980 rows: `EXPLAIN ANALYZE` shows an Index Only Scan Backward with
`rows=1000000` and 64 ms for the select alone. What every request pays: `Page` runs `select
count(b1_0.id) from book` for `totalCount`, a parallel index scan of the whole table, about 23 ms on
its own. On page 0 that count is most of the request.

## Q3. Which SQL statements does one `books` request run? How would you drop or bound the expensive one, and what does the client lose?

Two: `select ... order by added_at desc, id desc offset ? rows fetch first ? rows only` and `select
count(b1_0.id) from book b1_0`. (Spring Data skips the count only when a non-empty page comes back
shorter than `size`, because then it can work the total out.) Ways to drop it:

- Return a `Slice`, or a `Window` (lab 8.3). Spring fetches `size + 1` rows, so `hasNextPage` is free.
  The client loses `totalCount` and `totalPages`, so no "page 3 of 500".
- Count only when the client selects `totalCount`: check `env.getSelectionSet().contains("totalCount")`,
  or move the count to a separate field resolver so it runs only on demand. Clients that don't
  show a total stop paying for it.
- Cache the count for a minute, or use PostgreSQL's estimate (`reltuples`). That shows "about 1,000,000".
  The client loses exactness, which is usually fine for a feed and not for a billing screen.

## Q4. When is offset paging the right choice anyway?

When users jump to pages and the data is small or changes rarely: an admin table of 300 clubs with page
numbers, search results where nobody goes past page 10, a report that is generated once. There the
drift is rare and the deep-offset cost never arrives, and keyset cursors would take away "go to page
7". Offsets are also simple to cache by URL. Feeds that grow at the top and long lists are the case
for cursors.
