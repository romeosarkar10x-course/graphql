# Rubric, lab 8.2 notes (pass: 7 of 10)

| Points | Criterion |
|---|---|
| 3 | The table has real `EXPLAIN ANALYZE` numbers for all 12 cells (4 offsets x OFFSET / keyset / OR form), including the rows each scan read, and they grow the way the plans say they should: OFFSET and the OR form grow with the offset, the row-value keyset stays flat. Invented or copied numbers score 0. |
| 3 | Q1 explains *why* the OR form is slow in terms of the plan: a `Filter`, not an `Index Cond`, so the scan starts at the beginning of the index and discards rows. It names the fix (row-value comparison, or a redundant `published >= ?` prefix) and why that fix works. |
| 2 | Q2: what a keyset cursor cannot do that offset can (jump to page 37, show "page 12 of 50") and what product or UI choice that forces. |
| 2 | Q3: what happens to a cursor when its row is deleted, or when the sort key is updated, answered from how the SQL works (the position still exists as a value; it does not need the row). |
