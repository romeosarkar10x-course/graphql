# Rubric, game 4a notes.md (10 points)

| Criterion | Points | Full marks |
|---|---|---|
| Diagnosis | 3 | Real starter output; each broken invariant traced to retries/duplicates or to lost updates |
| Atomicity | 3 | Names the unit of atomicity, why it is correct, and a sound way to add parallelism (ordered locks, claim-then-run) |
| Retention | 2 | A retention rule tied to the retry window, with both failure directions |
| Answers vs totals | 2 | Explains why tying invariants to reported outcomes is stronger |

Pass: 7/10.
