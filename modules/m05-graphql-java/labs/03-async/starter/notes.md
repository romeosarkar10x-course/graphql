# Lab 5.3 notes

## Measurements

| Mode | n | ms |
|---|---|---|
| sync | 10 | |
| platform (4 threads) | 10 | |
| virtual | 10 | |
| platform (4 threads) | 1000 | (estimate first, then measure) |
| virtual | 1000 | |
| mutation (virtual) | 10 | |

## Questions

1. Why is `sync` 2,000 ms even though graphql-java's query strategy is called `AsyncExecutionStrategy`?
2. `CompletableFuture.supplyAsync(task)` without an executor runs on `ForkJoinPool.commonPool()`. Why is
   that a bad home for a 200 ms blocking call in a server? (Check its parallelism in the course image:
   `Runtime.getRuntime().availableProcessors()`.)
3. Virtual threads made 1,000 blocking calls cheap. Name one thing they do *not* fix (think about the
   database or service on the other end).
4. The mutation used the same async fetchers and still took 10 x 200 ms. Which part of graphql-java
   enforces that, and which spec rule is it obeying?
