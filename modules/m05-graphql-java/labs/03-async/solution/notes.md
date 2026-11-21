# Lab 5.3 notes

## Measurements (course image, 2026-11-21)

| Mode | n | ms |
|---|---|---|
| sync | 10 | 2003 |
| platform (4 threads) | 10 | 602 |
| virtual | 10 | 204 |
| platform (4 threads) | 1000 | estimate 250 waves x 200 = 50,000; measured 50,036 |
| virtual | 1000 | 226 |
| mutation (virtual) | 10 | 2007 |

## Questions

1. The strategy is asynchronous only in that it does not wait for a field's `CompletableFuture` before
   dispatching the next sibling. A fetcher that sleeps on the calling thread has already finished by the
   time it returns, so nothing ever overlaps: "async" is a property of the fetchers, not of the strategy.
2. The common pool is shared by the whole JVM (parallel streams, other libraries) and sized for CPU work:
   availableProcessors() - 1 threads (23 on the 24-core build machine). Blocking calls park those few threads, so unrelated work stalls and
   throughput caps at (cores - 1) / 0.2 s requests per second. Blocking work needs its own executor.
3. They do not add capacity downstream: 1,000 concurrent queries still hit a database pool of perhaps 10
   connections, and requests queue there instead. They also do not make the work cheaper, and code that
   pins its carrier thread (long native calls) still blocks a carrier.
4. `AsyncSerialExecutionStrategy`, which graphql-java uses for mutations: it waits for each root field's
   future to complete before starting the next. The spec's "ExecuteSelectionSet serially" for the mutation
   root: each field fully completes, in document order, before the next begins.
