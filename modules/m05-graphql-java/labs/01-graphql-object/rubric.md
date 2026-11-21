# Rubric, lab 5.1 notes.md (10 points)

| Criterion | Points | Full marks |
|---|---|---|
| Timings | 2 | Real numbers for both queries, medians, environment named |
| Growth | 2 | Names the phase that grows most and a plausible cause (AST size, validation rules walking every selection) |
| The limit | 2 | Quotes the error, names `maxTokens` = 15,000, links it to denial of service |
| Skipping validation | 2 | Explains the provider trick and the risk: executing a document never validated against this schema |
| Response shape | 2 | Correct on absent vs `null` vs object `data`, with the request-error vs field-error distinction |

Pass: 7/10.
