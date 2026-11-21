# Lab 8.2 notes

## EXPLAIN ANALYZE, 20 rows ordered by (published, id), 1,000,000 books

| Offset | OFFSET: time / rows read | Keyset (row value): time / rows read | OR form: time / rows read |
|---|---|---|---|
| 0 | 0.022 ms / 20 | 0.032 ms / 20 | 0.019 ms / 20 (1 removed) |
| 10,000 | 0.612 ms / 10,020 | 0.019 ms / 20 | 0.309 ms / 20 returned, 10,000 removed by filter |
| 100,000 | 5.859 ms / 100,020 | 0.040 ms / 20 | 2.836 ms / 20 returned, 100,000 removed |
| 900,000 | 53.029 ms / 900,020 | 0.036 ms / 20 | 26.634 ms / 20 returned, 900,000 removed |

Reference run, 2026-11-21, PostgreSQL 18.7, after `vacuum analyze` (`_workbench/2026-11-21-m08-probe/deep.txt`).
Before vacuuming, the index-only scans also had to fetch every heap page, and OFFSET 900,000 took about
525 ms from Java instead of 25 ms. A good answer may have different times; the rows read must match.

## Q1. Why is the OR form slow, when it returns the same rows? What fixed it?

The plan shows it. For the OR form, PostgreSQL uses the `(published, id)` index only for its order. It
has no `Index Cond`. It has a `Filter: ((published > X) OR ((published = X) AND (id > Y)))`, so the scan
starts at the first entry in the index and throws away every row before the position: 900,000 rows to
return 20. That is OFFSET again, with a WHERE clause on top (about half OFFSET's time in this run, still
growing with depth). A B-tree can start at one position only for a single
range on its leading columns. An OR of two ranges isn't one range, so the planner doesn't try.
`(published, id) > (X, Y)` is a single row-value range in the index's own order, and it appears as
`Index Cond: (ROW(published, id) > ROW(X, Y))`: a seek, then 20 rows. Adding a redundant
`published >= X and (...)` also works (the probe measured 0.177 ms and 4,000 rows filtered: the rest of
year X). Spring Data JPA 4.2 generates the OR form, which matters in lab 8.3.

## Q2. What can't a client do with keyset cursors that it can do with offsets? What does that mean for the UI?

A client can't jump. Page 37 has no cursor until you have walked pages 1 to 36, and "page 12 of 50"
needs a count and an offset. So keyset fits infinite scroll, "load more" and next/previous buttons. A
numbered pager needs offsets (fine for shallow pages: page 5 of a 20-row list is cheap) or precomputed
landmarks. Search results usually just stop at page 10. Total counts are a separate, expensive question
(lecture 8.3).

## Q3. A client holds a cursor for book 424242. The book is deleted, or its `published` year is corrected. What does the next page look like, and why?

Nothing breaks. The cursor holds the sort key `(1999, 424242)`, not a reference to the row, and
`where (published, id) > (1999, 424242)` is still a valid range when no row has that key. If the book is
deleted, the next page starts at the row after where it was. If its year changes to 2005, the cursor
still points at the old position (1999, 424242). The next page continues from 1999 and the book shows
up again later, when the reader gets to 2005: seen twice. The opposite also happens: a row ahead of the
cursor whose key moves behind it is never seen. A cursor that stores only the id would have to look the row up and fail when it is
gone. The sort key is what makes the cursor stable.
