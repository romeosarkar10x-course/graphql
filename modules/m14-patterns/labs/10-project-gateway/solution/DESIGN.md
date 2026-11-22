# Open Library gateway: design

## One page, one call

Every `book` and `books` field loads through one DataLoader per request, so the three aliases, the list and the
repeated ISBN in the grader's first page became one `/api/books` call with four distinct bibkeys. The batch
function first answers what the cache has fresh and sends only the misses. Search is not batchable (one query
string per call) and is cached by normalised text and size: "Le Guin" and "le guin" are one entry.

## Freshness and failure

An answer is fresh for `gateway.ttl` (10 minutes by default, 5 s in the test) and usable for `gateway.stale-for`
(a day). "Open Library has no such ISBN" is cached like any answer, or a page listing a missing ISBN would call on
every view. While Open Library fails, a usable answer is served with `stale: true` and no error: the reader gets
data that was right recently, and the field says so. An ISBN never fetched has nothing to stand in, so it is null
with `UPSTREAM_UNAVAILABLE` and the rest of the page renders.

## Being a good client

Timeouts (300 ms connect, 800 ms read), a User-Agent that names the gateway, and a gate: after any failure no call
for the cooldown (2 s), after a 429 for at least its `Retry-After`. The grader's five pages during a rate limit
made no calls at all. What it does not do: share the cache between instances (Redis would, Module 9), refresh
popular entries before they expire, or respect Open Library's published limits per IP across all users.
