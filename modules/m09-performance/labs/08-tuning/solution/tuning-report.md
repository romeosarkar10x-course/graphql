# Tuning report

Winner: virtual-g1

## The runs

`tune.sh` at 150 dashboards per second for 20 s after a 10 s warm-up, one run per configuration,
on the course container (JDK 25, 8 cores shared with PostgreSQL and Gatling). Two heaps, because the
first answer was surprising. These runs used the app before DocumentCache was added; the cache takes
about 20 us off each request, the same for every configuration. Full tables in `results/tuning.md`.

| Config | Heap | p50 | p95 | p99 | Failed | GC pauses (sum) |
|---|---|---|---|---|---|---|
| platform-g1 | 256 MB | 10 ms | 19 ms | 28 ms | 0 | 963 ms |
| virtual-g1 | 256 MB | 10 ms | 15 ms | 27 ms | 0 | 941 ms |
| platform-zgc | 256 MB | 20,001 ms | 60,000 ms | 60,001 ms | 2,984 | 8 ms |
| virtual-zgc | 256 MB | 20,001 ms | 45,301 ms | 60,000 ms | 2,966 | 22 ms |
| platform-g1 | 1 GB | 10 ms | 15 ms | 27 ms | 0 | 466 ms |
| virtual-g1 | 1 GB | 9 ms | 13 ms | 17 ms | 0 | 395 ms |
| platform-zgc | 1 GB | 12 ms | 16 ms | 21 ms | 0 | 3 ms |
| virtual-zgc | 1 GB | 11 ms | 16 ms | 21 ms | 0 | 4 ms |

### Five runs each at 1 GB (2026-11-24)

The same `tune.sh` five times with `HEAP=1g` (`_workbench/2026-11-24-finish/tuning/`), with the app as it is
now (DocumentCache in), on the same machine. Median of the five runs, with the range:

| Config | p50 (median, range) | p95 | p99 | Failed | GC pauses ms |
|---|---|---|---|---|---|
| platform-g1 | 9 (9-9) | 13 (13-15) | 18 (16-26) | 0 | 431 (419-487) |
| virtual-g1 | 9 (9-9) | 12 (12-13) | 16 (15-17) | 0 | 392 (359-434) |
| platform-zgc | 11 (11-12) | 15 (15-16) | 19 (18-23) | 0 | 4 (3-4) |
| virtual-zgc | 11 (11-12) | 15 (15-16) | 20 (19-21) | 0 | 4 (3-4) |

The single runs above were inside these ranges. Two things only the repeats show: p50 does not move at all
(9 ms for G1, 11 to 12 ms for ZGC, in every run), and platform-g1's p99 is the one number that jumps between runs
(16 to 26 ms), while virtual-g1's stays within 15 to 17 ms.

## What each change did

**ZGC on 256 MB fell over.** The GC log has 4,919 "Allocation Stall" lines, and the app log has
`OutOfMemoryError: Java heap space`, which killed Tomcat's acceptor thread: every request after that
timed out. ZGC collects concurrently and needs headroom to keep allocating while it works; with JFR
and async-profiler also allocating, 256 MB wasn't enough. G1 on the same heap paused (963 ms in total)
but kept up.

**At 1 GB, ZGC did what it promises**: total pauses of 3-4 ms instead of ~400 ms. But it didn't
improve the latency percentiles: p50 went up by 2 ms (ZGC's barriers and concurrent work cost CPU),
and p99 was 21 ms against G1's 17-27 ms. With pauses of a few ms each, G1's pauses are not what sets
this app's tail.

**Virtual threads** helped G1's tail at 1 GB (p99 27 to 17 ms) and changed little else. At 150 requests
per second Tomcat's 200 platform threads were never the limit; the database pool (10 connections) is
the shared resource either way.

## Why this winner

virtual-g1 had the lowest p50, p95 and p99 at 1 GB and no failures at either heap size. G1 is the
JDK default and safe on small heaps, which matters more for a 256 MB container than ZGC's pause times.

## What would change the answer

Five runs per configuration at 1 GB settle what one run could not: G1's p50 is 2 ms below ZGC's in every run,
and virtual-g1 has the steadiest tail (p99 median 16 ms, range 15 to 17). The gaps between the two ZGC variants
(1 ms) are noise. So the winner stands, now for a measured reason; the honest summary is still "G1 or ZGC at 1 GB
are both fine; ZGC at 256 MB is not" (not repeated: that failure is not a matter of milliseconds). A heap of several GB, an app with large live data (big caches), or a latency SLO at
p99.9 would favour ZGC. A much higher request rate, or slow downstream calls holding threads, would
show virtual threads' real benefit.
