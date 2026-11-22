# Lab 15.5 notes

## Predicted against actual

My first predictions (before running anything) and the router's plans:

| query | I predicted | the plan | why it differs |
|---|---|---|---|
| q1 | `[catalog]` | `[catalog]` | |
| q2 | `[catalog], [reviews]` | `[catalog], [reviews]` | |
| q3 | `[reviews], [accounts]` | `[reviews]` | `@provides(fields: "handle")`: reviews answers the handle itself |
| q4 | `[reviews], [accounts], [catalog]` | `[reviews], [accounts, catalog]` | both only need the review's keys, so they run in parallel |
| q5 | `[clubs], [catalog, accounts]` | `[clubs], [accounts, catalog], [clubs]` | `weeksToRead @requires(fields: "pages")`: clubs again, after catalog |
| n1 | one request per member | `[clubs], [accounts, reviews], [catalog]` | the router batches a whole level into one `_entities` call |

Two of six right at first. The plans are read from the supergraph alone: `@provides` removes a step, `@requires`
adds one, and independent entity fetches share a step.

## Cross-subgraph N+1

n1 asks for 3 clubs, 17 memberships, every review those members wrote and the book of each review. The test
counted one HTTP request to each subgraph (accounts 1, catalog 1, reviews 1, clubs 1): the router collects every
representation at a path (`clubs.@.members.@`) into one `_entities` request. The N+1 that remains is inside a
subgraph: whether reviews answers 17 `User` representations with one query or 17 (lab 15.4). Across subgraphs
the cost is steps, not rows: a deeper query adds sequential round trips, and the slowest subgraph in each step
sets the pace.

## Composition in CI

`ci/compose.sh` runs on every pull request that touches `subgraphs/`; a change that does not compose fails the
check before review, and the last good supergraph is never overwritten. The artifact is what routers load. What
CI cannot see from SDL alone: whether a change breaks clients (that needs usage data, Module 13.5 and 16.2), and
whether subgraphs actually serve what their SDL says (that is lab 15.4's `_service` check against the registry).
