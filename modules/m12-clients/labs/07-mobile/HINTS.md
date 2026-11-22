# Hints: m12/07

1. Apollo Kotlin does not throw on a network failure: `execute()` returns a response whose `exception` is an
   `ApolloNetworkException`. That is your signal to queue. A tab-separated line per review in `outbox.txt` is
   enough.
2. If the card read from the cache after a successful sync still shows the old average, the mutation's book
   and the card's book are different records. How does Apollo Kotlin decide a record's key when you tell it
   nothing? Look up `@typePolicy` and put it in a schema extension file next to `schema.graphqls`.
3. In `sync`, replay the lines in order, keep those that fail at the network, and treat `DUPLICATE` as done
   (lab 12.8). Finish by reading the card with `FetchPolicy.CacheOnly` to prove the cache, not the network,
   has the new value.
