# Demand control: notes (reference)

## Tuning: what each limit is set to, and why

Real costs measured with this solution's `extensions.cost` (`_workbench/2026-11-21-m10-probe/lab04-real-costs.txt`):
Home 430, Club 245, Me 223, Readers 160, Catalogue 150, Book 108. Depth: the deepest screen (Club page,
`club > reviews > book > title`) is 4; Home is 4.

| Limit | Value | The real query closest to it | The attack it stops |
|---|---|---|---|
| depth | 10 | Home and Club, depth 4: room for 6 more levels of a future screen | depth 12+ (X01); lab 10.3's 98-level query |
| fields | 500 | Club page, about 12 selected fields; generated pages rarely pass 100 | 300 aliases of a 2-field selection, 600 fields (X02) |
| cost | 5,000 | Home, 430: a tenth of the limit | fan-out at depth 7, cost 43,388,772 (X03), also through fragments (X04) |
| first | 50 | Catalogue asks for exactly 50 | `books(first: 1000)` (X05), negative pages (X06) |
| timeout | 1 s | Book page with recommendations, 0.31 s | ten recommender calls, 3 s of work, cancelled at 1 s (X07) |
| budget per minute | 10,000 | 23 Home pages a minute per reader | the fifth 2,110-point query in a minute; a brute force or a scraper |

Depth and field counts are cheap to check (validation, before any resolver runs) but say little about
cost: X03 passed both and would have made 4 million fetches. The cost limit is the one that matters; the
others are cheap backstops. Tuning was: start at 10x the most expensive real screen, then lower until
an attack from lab 10.3 still failed and no real screen got within 2x.

## Your cost function, and one query it prices badly

Each field costs 1 (10 for `recommendations`), plus its children, multiplied by `first` for lists. It
prices by the page size asked for, not returned: `club(id: "c3") { members(first: 50) }` costs 50 even for
a reader who cannot see c3 and gets null. It also charges a deep cheap path (`viewer.clubs.members`) the
same as an expensive one with the same shape; only `recommendations` has a real weight, because only
it was measured as slow. A better function would use measured per-field latency (lecture 9.7).

## What the limits cost a legitimate client

A screen that grows (a bigger page, one more level) can start failing with a validation error the
developer never saw locally, so the limits and each request's cost must be visible: `extensions.cost`
on every response (R07), the limits in the docs, and a test that prices every real operation in CI. A
burst of legitimate traffic from one office behind a NAT shares the anonymous IP bucket. The timeout
cancels partly done work, so a mutation must not be cancelled halfway (it is safe here because every
mutation is a single map update).
