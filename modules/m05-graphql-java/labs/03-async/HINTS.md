# Hints, lab 5.3

1. graphql-java starts a sibling's fetcher when the previous fetcher *returns*. A fetcher that returns a
   `CompletableFuture` that is not finished yet lets the next one start straight away.
2. `CompletableFuture.supplyAsync(supplier, executor)` runs the supplier on that executor. Leave out the
   executor and you get `ForkJoinPool.commonPool()`, which is question 2 in the notes.
3. `java.util.concurrent.Executors` has a factory for an executor that starts a new virtual thread for
   every task (JDK 21+). Nothing else in the code has to change.
