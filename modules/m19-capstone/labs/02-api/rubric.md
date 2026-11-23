# Rubric, stage 2 (10 points)

| Criterion | Points | Full marks |
| --- | --- | --- |
| Contract first | 2 | The schema is loaded from `contract/`; no second copy; the served schema matches |
| The suite tests behaviour | 3 | Typed failures, page ends (`hasNextPage: false`), invalid input, an idempotent repeat, not only happy paths |
| Storage | 2 | Keyset pagination with indexes that match the sorts; constraints in the database for what must never happen |
| Java | 3 | Records and sealed types for the model; resolvers thin, rules in plain classes with unit tests |

Pass: 7/10.
