# Seeing N+1: notes

## Predict, then measure

| Query | Predicted | Measured | Formula |
|---|---|---|---|
| Dashboard (contract/dashboard.graphql) | 1,250 | 1,250 | 1 + 10 x (owner + currentBook + ownerPicks + memberCount + members) - 1 (club 10 has no book) + 10 x 20 reviews + 200 x 5 books |
| `club(id: 1) { name owner { name } members(first: 3) { handle } }` | 3 | 3 | 1 club + 1 owner + 1 members; the members' handles come with the members |
| `clubs(first: 3) { members(first: 2) { reviews(first: 2) { book { title } } } }` | 22 | 22 | 1 + 3 + 3 x 2 + 3 x 2 x 2 |

## The general rule

Each list multiplies the statements below it. With fan-outs f1, f2, f3 the naive count is
1 + f1 + f1 f2 + f1 f2 f3 for one field per level, plus one statement per to-one field per parent.
The dashboard's 1,000 book loads are 10 x 20 x 5: the deepest field dominates.

## Where the time goes

The dashboard took about 200 ms warm for 1,250 statements: about 0.16 ms per round trip on a local
database. Over a network with 1 ms per round trip it would be well over a second. The count, not the
SQL, is the problem: every statement here uses an index.
