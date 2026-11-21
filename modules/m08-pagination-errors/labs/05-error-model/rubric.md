# Rubric, lab 8.5 notes (pass: 7 of 10)

| Points | Criterion |
|---|---|
| 3 | Q1 lists every wrong prediction honestly (expected vs actual) and explains each with the propagation rule: a null at a non-null position goes to the nearest nullable ancestor, and list item nullability counts. "All right" is fine only if the prediction file was clearly written first; zero misses with no reasoning scores at most 1. |
| 3 | Q2 separates request errors (no `data` key, nothing executed, not retryable, client bug) from execution errors (`data` present, possibly `null`), with what a client should do with each, and mentions the HTTP status difference. |
| 2 | Q3 explains E3 vs E7 from `[User!]!` vs `[Review]`, and connects it to the cost on each side (null checks vs blast radius). |
| 2 | Q4 names concrete leaks from the default messages (service names, ids, internal wording; real exceptions leak more) and what should replace them. |
