# Rubric, lab 12.5 notes (pass: 6 of 9)

| Points | Criterion |
|---|---|
| 3 | The real store from the live run, the record both pages share, and why the mutation reached them (it selected `__typename` and `id`). Invented output scores 0. |
| 3 | Two concrete schema-blind cases (defaults, `book(id:)` vs a list, entity identity, possible types) and how a client library fixes each. |
| 3 | What stays stale after `createReview` (the reviews list), why no cache can infer it, and at least three options. |
