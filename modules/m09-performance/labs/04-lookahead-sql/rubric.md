# Rubric, lab 9.4 notes (pass: 7 of 10)

| Points | Criterion |
|---|---|
| 3 | Median and p95 for naive, batched and lookahead, measured the same way after a warm-up. Invented numbers score 0. |
| 3 | Explains the comparison from the SQL: what MULTISET becomes on PostgreSQL (correlated JSON aggregation), and why fewer statements can be slower (cheap local round trips, JSON building and parsing). |
| 2 | When lookahead wins: expensive round trips, filtering or aggregation in the database, skipping whole joins. |
| 2 | At least two costs: aliases with different arguments, SQL duplicated beside the resolvers, ordering and limits that must agree with the loaders, or a fetcher that has to know its whole subtree. |
