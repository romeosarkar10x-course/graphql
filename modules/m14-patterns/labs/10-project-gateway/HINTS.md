# Hints: m14/10

1. A DataLoader per request batches everything one execution level asks for: register a mapped batch loader with
   Spring's `BatchLoaderRegistry` and take `DataLoader<String, ...>` as a controller argument. Aliases at the root
   are one level; `loadMany` serves lists. The batch function is where the cache sits: answer hits, fetch misses.
2. Cache what Open Library said, including "nothing": store an `Optional` with the time you fetched it. Two ages
   matter: fresh (serve, no call) and usable (serve only when the call fails, marked stale).
3. A failure tells you something about the next few seconds, not just this call: remember until when you will not
   call (the cooldown, or `Retry-After`) and check it before calling. Return the "nothing usable" case as a
   `DataFetcherResult` with an error on that field's path, so the other fields keep their data.
