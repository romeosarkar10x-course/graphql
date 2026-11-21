# Rubric, lab 9.3 notes (pass: 7 of 10)

| Points | Criterion |
|---|---|
| 3 | Statements and warm latency measured for naive and batched, plus what happened without the chaining fix; a chart or table that makes the ratio visible. Invented numbers score 0. |
| 3 | Explains why fields with arguments can't use @BatchMapping as is (the key must include the argument) and how one SQL statement serves keys with different `first` values. |
| 2 | Explains the chained-loader hang: per-level dispatch, a load made after its level was dispatched, and what the flag changes. |
| 2 | Names one cost of batching: e.g. the grouping work in Java, one big `in (...)` list, or loaders hiding which statements run. |
