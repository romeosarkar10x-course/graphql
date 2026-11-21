# Performance report: the club dashboard, before and after batching

## Setup

Same machine, same data (bin/course clubs, scale 1), same load: `measure.sh` warms the app for 10 s,
then Gatling sends 20 dashboards per second for 20 s as an open workload (400 requests). Before: the
naive resolvers of lab 9.1. After: the loaders of lab 9.3. Raw output in `results/`.

## Latency (Gatling, client side)

| | Statements per request | p50 | p95 | p99 | Failed |
|---|---|---|---|---|---|
| Before (naive) | 1,250 | 51 ms | 60 ms | 64 ms | 0 |
| After (batched) | 8 | 14 ms | 17 ms | 18 ms | 0 |

Batching cut p50 by 3.6x and p99 by 3.6x. The spread is small in both runs because 20 requests per
second is far below what either version can serve: there is no queueing yet.

## What the server's own timer said (Micrometer, graphql.request)

| | p50 | p95 | p99 |
|---|---|---|---|
| Before | 48 ms | 350 ms | 904 ms |
| After | 12 ms | 19 ms | 520 ms |

The p50s agree with Gatling. The p99s don't: Micrometer's percentiles come from a histogram over a
sliding window (2 minutes by default), and the window still held the warm-up, when the JIT had not
compiled anything yet. Client-side percentiles from the measured phase only are the ones to report.
Server percentiles also can't be averaged across instances.

## Where the time goes

`jfr view hot-methods` (results/*-hot-methods.txt): before, the top of the list is HashMap work and
Spring's AOP proxy around the repositories: 1,250 repository calls per request, each through a proxy,
a transaction and a statement. After, graphql-java's PerLevelDataLoaderDispatchStrategy and the
lambda that groups rows by key in BatchController appear near the top: the grouping is a filter over
all rows per key (keys x rows), which the flame graph shows as a wide frame under `members` and
`reviews`. A `groupingBy` would make it linear; at 200 keys it is not worth much yet.

## What I'd measure next

The same load at 100 and 200 requests per second, to find where p99 breaks away from p50 (lab 9.8), and
the database's own time (pg_stat_statements) to separate SQL time from engine time.
