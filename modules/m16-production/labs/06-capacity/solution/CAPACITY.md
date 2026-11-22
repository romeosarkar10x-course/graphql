# Capacity

Pod capacity: 42700 cost/s

One "pod" here is the app pinned to one CPU (`taskset -c 0 java -XX:ActiveProcessorCount=1 -jar ...`), the way a
1-CPU limit would hold it, on a 24-core machine whose other cores ran the load. The page is the club page
(`contract/club-page.json`): club, members, current book with its rating, and the reading list with reviews; cost 61 by the lab's rule (`extensions.cost` says so). Five seconds per step, open model (`contract/ramp.sh`).

## The knee

| Rate (club pages/s) | p50 | p95 | p99 |
| --- | --- | --- | --- |
| 100 | 3 | 7 | 14 |
| 200 | 1 | 4 | 9 |
| 400 | 1 | 6 | 31 |
| 600 | 0 | 7 | 28 |
| 800 | 0 | 67 | 86 |
| 1000 | 8 | 320 | 380 |

p99 stays under 40 ms up to 600 pages a second and breaks between 600 and 800: at 1000 the median itself starts
to queue. I call the knee 700 pages a second, so 700 x 61 = 42,700 cost per second per pod. A first run on the
same build put 800 at p95 38 / p99 89, so the knee moves by a step between runs; one run per step is not enough
for more precision than that.

## One optimisation, and the knee again

Every request was parsed three times: once by graphql-java, once by the cost instrumentation and once by the
usage reporter, and validated every time. I added a parsed-document cache for the two instrumentations and a
`PreparsedDocumentProvider` (through a `GraphQlSourceBuilderCustomizer`), so each distinct document is parsed and
validated once (`_workbench/2026-11-22-m16-build/parse-once-app`).

| Rate | p50 | p95 | p99 |
| --- | --- | --- | --- |
| 800 | 0 | 6 | 31 |
| 1000 | 0 | 9 | 24 |
| 1200 | 2999 | 3000 | 3000 (every request timed out) |

The knee moved from about 700 to about 1,000 pages a second (+40%), and then the service fell off a cliff: at
1,200 nothing came back within 3 s. A cliff instead of a slope is the reason to keep the target well below
capacity. I kept the reference app unchanged; this lab's capacity number is the one measured before the change.
A cache keyed by the document text is unbounded unless capped (mine clears at 1,000 entries); with persisted
documents the set is known and small.

## The autoscaler target, and why

Target 32k cost/s per pod, 75% of capacity. `HpaSim` over the 25-minute profile, against other targets:

| Target | Peak replicas | Seconds overloaded | Pod-seconds | Final |
| --- | --- | --- | --- | --- |
| 16k (37%) | 12 (the max) | 0 | 13,002 | 5 |
| **32k (75%)** | 8 | 15 | 7,814 | 3 |
| 42k (98%) | 6 | 214 | 5,970 | 2 |
| 64k (150%) | 4 | 566 | 4,440 | 2 |

The headroom pays for the 30 s a new pod takes to serve and the 15 s between decisions: at 98% the ramp outruns
the scaler for three and a half minutes. At 37% it never overloads, but it pays two thirds more pod time and
still holds 5 pods ten minutes after the peak (the 300 s scale-down window keeps the highest recent recommendation).
Cost per second is a better signal than CPU here because the abusive pattern from the game day (a few huge
queries) shows in cost long before the CPU average moves.

## Edge caching and multi-region reads: what would they change?

The club page is public and identical for everyone, so a CDN in front of persisted GET queries (Module 9) would take
most of its traffic off the pods; the autoscaler would then scale on what is left, mostly mutations and
per-user queries. Multi-region reads would put replicas near users but split the capacity: each region needs its
own minimum and headroom, and the reviews service would need replicas there too, or every page pays the
cross-region round trip on `Book.reviews`.
