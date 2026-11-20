# Lab 4.1 notes (reference)

## Serial vs parallel

Reference run: query total 228 ms, all three starts within 1 ms (20, 21, 21); mutation total 602 ms,
b starts at 201 when a ends, c at 401. The starter's query took 628 ms: its fetcher sleeps on the
calling thread and returns a plain value, so each field is finished before graphql-java dispatches the
next. "May run in parallel" requires fetchers that return futures.

## Why serial?

`mutation { a: withdraw(amount: 50) b: withdraw(amount: 70) }` on a balance of 100: serial gives one
success and one "insufficient funds", deterministically, in document order. Parallel lets both read 100
and both succeed (or fail depending on timing). Clients write mutations in the order they mean.

## Selecting data back

With `Boolean` returns: 4 mutations plus at least 3 queries (club count, book average twice, club count
again), 7+ requests and races between them. A normalised cache updates `Club:c2.memberCount` and
`Book:b2.averageRating` from the payloads by id, so every screen showing them refreshes for free.

## Question

That fields of one query run in any particular order, or sequentially: no shared mutable state between
resolvers without synchronisation, no "this field ran first, so the cache is warm".
