# Hints, lab 5.8

1. `PreparsedDocumentProvider.getDocumentAsync(input, parseAndValidate)`: return a cached
   `PreparsedDocumentEntry` when you have one; otherwise call the function, which parses and validates,
   and cache what it returns. Register it with `GraphQL.Builder.preparsedDocumentProvider`.
2. `Caffeine.newBuilder().maximumSize(n).build()` gives a `Cache<K, V>` whose `get(key, k -> compute)`
   computes at most once per key. What is the key? Not the whole `ExecutionInput`.
3. `ExecutionInput` has a `cancel()` method. Something has to call it while `execute` is still running,
   which means another thread: `TIMER.schedule(...)`. Do not forget to cancel the timer when the query
   finishes first.
