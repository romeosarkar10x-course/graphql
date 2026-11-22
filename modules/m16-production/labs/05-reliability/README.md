# Lab 16.5: Reliability: kill the reviews dependency at peak

Lecture: [16.5 Reliability](../../lectures/16.5-reliability.md). Grading: auto.

Every `Book.reviews` and `Book.averageRating` comes from the reviews service. In the starter, `ReviewsClient`
waits as long as that service takes. When the service slows down, the whole club page waits with it: the
title, the members, everything. Make the page degrade instead of stall:

- **Timeouts**: a short connect and read timeout on the reviews calls (not on the subscription feed's long poll).
- **A circuit breaker** (Resilience4j 2.4.0, `resilience4j-circuitbreaker`): stop calling the service when calls
  keep failing or are slow, and try again a little later.
- **A bulkhead** (`resilience4j-bulkhead`): cap concurrent calls, so a slow dependency cannot take every thread.
- **Graceful degradation**: when a call is refused or fails, `reviews` and `averageRating` are `null`. Each gets
  an error with `extensions.code: REVIEWS_UNAVAILABLE` at its own path, and the rest of the page still renders.
  The schema already allows `null` there.

The test's experiment, with `contract/Peak.java` (open model, so a slow server cannot slow the load down):

| Time | The reviews service |
| --- | --- |
| 0-4 s | normal |
| 4-8 s | every call takes 2 s |
| 8-12 s | killed |

100 club pages a second arrive throughout. Targets:

- every page renders;
- p95 under 250 ms and p99 under 1 s;
- fewer than 100 calls reach the slow service in its 4 slow seconds;
- the page recovers within 20 s of the service coming back.

```bash
bin/course start m16/05
cd workspace/m16-production/05-reliability
bin/course test m16/05
```

Run the load yourself (`bin/course shell`, `source` on `ops.sh`, `reviews_start`, start your jar):
`bin/course java /course/modules/m16-production/contract/Peak.java --rate 100 --seconds 12`. Switch the service with
`reviews_fault slow 2000`, `reviews_fault error` and `reviews_fault ok`. Its `/_stats` counts calls.

## Experiment

Run the starter through the same load first and note its p95 per second. Then add one defence at a time:
timeout only, then breaker, then bulkhead. Which one moves which number? graphql-java can also cancel a whole
request (`ExecutionInput`'s `cancel()`, used in Module 8); where would that help that these do not?

## Done when

`bin/course test m16/05` passes.
