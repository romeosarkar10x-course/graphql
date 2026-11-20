# Hints, lab 4.1

1. A mutation's payload is an object type like any other: select into it. `joinClub(input: {clubId: $club}) { club { name memberCount } userErrors { code } }` returns the new count in the same response. `deleteReview` returns the `book`, so you can read the new average there.
2. The review id is not known in advance: read it from `createReview`'s payload and pass it as a variable to `deleteReview`. Always check `userErrors` (and top-level `errors`) before trusting the payload.
3. graphql-java's query strategy (`AsyncExecutionStrategy`) starts every root field's fetcher before waiting for any, but a fetcher that sleeps on the calling thread finishes before it returns. Return `CompletableFuture.supplyAsync(() -> ..., POOL)` and the three sleeps overlap. The mutation strategy (`AsyncSerialExecutionStrategy`) waits for each field's future before starting the next.
