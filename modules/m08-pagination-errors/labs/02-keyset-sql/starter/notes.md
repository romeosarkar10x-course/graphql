# Lab 8.2 notes

## EXPLAIN ANALYZE, 20 rows ordered by (published, id), 1,000,000 books

| Offset | OFFSET: time / rows read | Keyset (row value): time / rows read | OR form: time / rows read |
|---|---|---|---|
| 0 | | | |
| 10,000 | | | |
| 100,000 | | | |
| 900,000 | | | |

## Q1. Why is the OR form slow, when it returns the same rows? What fixed it?

## Q2. What can't a client do with keyset cursors that it can do with offsets? What does that mean for the UI?

## Q3. A client holds a cursor for book 424242. The book is deleted, or its `published` year is corrected. What does the next page look like, and why?
