# Lab 15.7: Operating a federated graph: moving a field

Lecture: [15.7 Operating a federated graph](../../lectures/15.7-operating.md). Grading: auto + rubric. Needs the
federation tools (`bin/course fed`).

`Review.book` lives in catalog (its own `Review` stub with a `book` field). It belongs in reviews. Move it with
**zero client errors** while traffic flows. `states/1/` is today: `catalog.graphqls` and `reviews.graphqls`.
Add `states/2/`, `states/3/`, ... each a complete copy of the two files, one deploy apart:

- every state composes;
- from one state to the next, **exactly one subgraph changes** (one team deploys);
- the last state has `Review.book` in reviews only, and no `@override` left behind.

The grader plays operations: it runs the contract subgraphs with each state's SDL, routes through Apollo Router
with hot reload, deploys each step blue/green (the new subgraph version on a new port, a new supergraph pointing
at it, then the old one stopped), and sends `{ latestReviews { book { title } } }` every 50 ms throughout. One
error, or a null book, fails the lab. Finally it checks the last plan fetches `Book` from catalog by key, not
`Review` from catalog.

Then write `RUNBOOK.md`: the steps, what to check before each one, and how to roll each one back.

```bash
bin/course start m15/07
cd workspace/m15-federation/07-operating
bin/course test m15/07
```

## Done when

`bin/course test m15/07` passes and `RUNBOOK.md` passes the rubric (`bin/course grade m15/07`).
