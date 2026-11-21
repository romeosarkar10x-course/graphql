# Seeing N+1: notes

## Predict, then measure

Write your prediction before you run the query.

| Query | Predicted | Measured | Formula |
|---|---|---|---|
| Dashboard (contract/dashboard.graphql) | | | |
| `club(id: 1) { name owner { name } members(first: 3) { handle } }` | | | |
| `clubs(first: 3) { members(first: 2) { reviews(first: 2) { book { title } } } }` | | | |

## The general rule

## Where the time goes
