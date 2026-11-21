# Lookahead: notes

## One statement against eight

100 dashboards after 20 warm-up requests, same machine, same data (`_workbench/2026-11-21-m09-probe/compare.txt`):

| Version | Statements | Median | p95 |
|---|---|---|---|
| Naive (9.1) | 1,250 | 43.6 ms | 54.9 ms |
| Batched (9.3) | 8 | 11.6 ms | 17.3 ms |
| One statement (9.4) | 1 | 15.2 ms | 19.3 ms |

The single statement is **slower** than eight batched ones here. jOOQ turns each MULTISET into a
correlated subquery with `jsonb_agg(jsonb_build_array(...))`: PostgreSQL builds 10 x 20 x 5 small JSON
arrays, then the app parses JSON instead of reading plain rows. The eight batched statements are each
one index range scan returning flat rows. Round trips to a local database cost about 0.1 ms, so saving
seven of them saves little.

## When lookahead wins

When the round trip is expensive (a database 1-5 ms away, or a remote service), when the database can
filter or aggregate what the loaders would fetch and throw away, or when the selection lets you skip a
join entirely (`clubs { name }` reads one table). It loses when the tree is wide and deep and the
database has to assemble nested JSON.

## Its costs

The fetcher has to know the whole subtree: aliases with different arguments (L04) can't share one
MULTISET, so the code falls back to loaders; every new field needs SQL here as well as a resolver;
and the lookahead and the loaders must agree on ordering and limits, or the same query gives two
answers depending on the path.
