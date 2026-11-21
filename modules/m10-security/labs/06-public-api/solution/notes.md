# Public API security: notes (reference)

## Spender's run: requests, throttles, time

Measured 2026-11-21 (`_workbench/2026-11-21-m10-probe/lab06-spender.txt`), key `pk_spender_3`: 1,000 points,
restoring 1,000 a second; the catalogue query costs 140.

| Client | Answered | Throttled | Time |
|---|---|---|---|
| a naive loop of 40 requests | 8 | 32 | 0.28 s |
| Spender, 40 requests | 40 | 0 | 5.4 s |

The naive loop drained the bucket in 7 requests and then wasted 32 round trips on errors (and would have
failed its job). Spender's floor is (40 × 140 − 1,000) / 1,000 = 4.6 s of restoring; it took 5.4 s because it
waits exactly `(cost − available) / restoreRate` before each request it cannot pay for, from the numbers the
last response carried. Near the end it ran at 5 points left, about one request per 135 ms: the budget, not
the client, set the pace.

## Why GitHub and Shopify price queries instead of counting requests

One GraphQL request can be one field or a million (lab 10.3's fan-out), so "5,000 requests an hour" says
nothing about load. GitHub gives each user 5,000 points an hour, computed from the connections' `first`
sizes, and exposes `rateLimit { cost remaining resetAt }` in the schema. Shopify's Admin API uses a leaky bucket
(a maximum and a restore rate per second) and returns `extensions.cost` with `requestedQueryCost`,
`actualQueryCost` and `throttleStatus` on every response; this lab copies that shape. Both let a client
plan instead of guessing, which is what made Spender possible. (Their numbers are from their docs as of
2026-11-21; not measured here.)

## Keys, scopes and users: who is responsible for a bad request

The key names the app, the token names the person; both rules apply (P06: the review bot has
`write:reviews`, but ben still may not write to c1). The budget belongs to the app, so one abusive
integration cannot spend another's, and the app's owner answers for its traffic. Scopes limit the blast
radius of a leaked key: `pk_reader_1` leaked can read public data and nothing else. A key in a mobile app
is not a secret (anyone can extract it), so it identifies, it does not authenticate.

## Abuse you would watch for in /_admin/usage

A throttled share that stays high (a client that never backs off, like the naive loop: 32 of 40); a key
whose cost per request jumps (a scraper walking every page at `first: 50`); one key used from many IPs
(leaked); a write scope used at night by a read-mostly integration. The response is to slow first (lower
the restore rate for that key), then revoke.
