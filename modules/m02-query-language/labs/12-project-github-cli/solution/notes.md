# Lab 2.12 notes (reference)

## REST vs GraphQL for this card

At least three REST calls: `GET /users/{login}` (name, company, location, followers, following,
public_repos), and pinned repositories are not in REST at all; the closest is
`GET /users/{login}/repos?sort=stars` plus one call per repository's languages if you want more than
the primary language. Yearly contributions are GraphQL-only (`contributionsCollection`). So REST gives
a different, weaker card in 2+ round trips; GraphQL gives the real card in one.

## What the request costs

GitHub's GraphQL rate limit is points per hour, and a query's cost is computed from the number of
nodes it could return (connections' `first` arguments multiply). A cheap card costs about 1 point; a
query with `first: 100` nested inside `first: 100` costs far more. Charging per request would let one
huge query cost the same as `{ viewer { login } }`: the same problem lab 2.8's analyzer measures.
(Record the measured cost here when you run it with your token.)

## Errors

An unknown login gives HTTP 200 with `"data": {"user": null}` and an error whose `type` is `NOT_FOUND`
(GitHub's own field beside `message`). The CLI treats "user is null and every error is NOT_FOUND" as
exit 3; anything else without a user is a failure (exit 4). HTTP status alone cannot tell them apart.
