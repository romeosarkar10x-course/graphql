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

### The same HPA on a real cluster (2026-11-24)

`k8s/hpa.yaml` applied to kind 0.33.0 (Kubernetes 1.37.0) with lab 16.4's Deployment, a Prometheus scraping the
pods and prometheus-adapter 0.12.0 serving `graphql_cost_per_second` from `k8s/cost-per-second.promql`
(`_workbench/2026-11-24-finish/k8s/`). One change: the target was lowered to `500` so four in-cluster clients
(a 901-cost query every 50 ms each) could cross it; 32k needs a busy CPU per pod. Replicas went 2, 4, 8, 12 in
45 s (sampled every 15 s), within the default scale-up limit of 100% or 4 pods more per 15 s, and stopped at `maxReplicas`; per-pod cost then settled at
about 5,400/s, the same total spread over 12. After the load stopped the metric read 0 within 30 s, and the
replicas stayed at 12 for 5 minutes (the default `scaleDown.stabilizationWindowSeconds` of 300) before dropping
to 2 in one step. `HpaSim.java` uses the same two rules: that scale-up limit and the 300 s window (it reads
`behavior.scaleDown.stabilizationWindowSeconds` too); the cluster stayed within both.

## Edge caching and multi-region reads: what would they change?

The club page is public and identical for everyone, so a CDN in front of persisted GET queries (Module 9) would take
most of its traffic off the pods; the autoscaler would then scale on what is left, mostly mutations and
per-user queries. Multi-region reads would put replicas near users but split the capacity: each region needs its
own minimum and headroom, and the reviews service would need replicas there too, or every page pays the
cross-region round trip on `Book.reviews`.
