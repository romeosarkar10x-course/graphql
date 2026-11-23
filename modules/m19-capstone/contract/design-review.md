# The capstone design review

Stage 1's review, built from Module 7's lessons. The reviewer (a friend in the group variant, an AI agent in the
solo variant: `bin/course grade m19/01`) reads the pitch, the design doc, the contract and the operations, and
writes the review into the repository's `docs/review` file. The author answers each point under it.

Score each row 0 to 2: 0 missing or wrong, 1 present but with a problem the reviewer can name, 2 sound.

| # | Question | Lecture |
| --- | --- | --- |
| 1 | **Client first.** Does every operation map to a screen or a job, and does every root field serve one? | 7.1 |
| 2 | **Names.** Do names say what the client sees, not the tables (no `tbl_`, `_id` suffixes, CRUD verbs for product actions)? | 7.1, 7.7 |
| 3 | **References.** Do fields return objects rather than keys, with every exception argued? | 7.3 |
| 4 | **Nullability.** Is every `!` a promise the product keeps when part of it fails, and is every nullable field explained? Ask what one subgraph's outage does to a screen | 7.4 |
| 5 | **Abstract types.** Is each interface and union the right one of the two, and is the `@oneOf` input used where exactly one way of asking applies? | 7.5 |
| 6 | **Evolution.** Can the next three features you can imagine be added without a breaking change? Are inputs and results objects, not bare scalars? | 7.6 |
| 7 | **Errors.** Are expected failures data the client can show, and are request errors and access separated from them? | 4.3, 8.4, 8.5 |
| 8 | **Pagination.** Are growing lists connections with a page limit, with filters and sorts the screens need and no others? | 8.2, 8.3 |
| 9 | **Authorization.** Is every field's rule stated, and can a client learn anything (existence, counts) it should not? | 10.2 |
| 10 | **Boundaries.** Does each subgraph own its data, and is the entity between them the one a team would choose? | 7.7, 15.1 |

Pass: 14 of 20, with no row at 0. The review is at least 200 words of its own and ends with a verdict: ready
for stage 2, or the points to fix first.
