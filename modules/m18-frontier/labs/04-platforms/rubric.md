# Rubric: 18.4 An API you did not write (GAPS.md)

Pass mark: 8 of 14.

| Points | Criterion |
| --- | --- |
| 3 | Schema shape: generated names (`readerByOwnerId`, `rowId`), the `Reader` versus `User` vocabulary, nullability from SQL, what clients would see change if a column is renamed |
| 2 | Ordering and filtering only on indexed columns (no `RATING_DESC`), and what that does to a product's screens |
| 3 | Security: authorization (row-level security or a service in front), demand control (no depth or cost limit in the config), trusted documents, error masking |
| 2 | Writes: why a generated CRUD mutation is not a product mutation (Module 4: payloads, user errors, idempotency) |
| 2 | One other platform compared, with a source: what it generates, how it is extended, its licence |
| 2 | A verdict: where a generated API is the right call (an internal admin tool, a prototype), and where it is not |
