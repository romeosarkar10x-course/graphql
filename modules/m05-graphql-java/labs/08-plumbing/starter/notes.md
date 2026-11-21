# Lab 5.8 notes

## Throughput (`java -jar target/shelfie-plumbing.jar`)

| | req/s | parses |
|---|---|---|
| no cache | | |
| Caffeine cache | | |

## Questions

1. The cache key is the query text alone. Why are variables not part of it, and what would happen to
   the hit rate if clients inlined values into the query string instead of using variables?
2. Why must the cache be bounded? Describe the request pattern an attacker would use against an
   unbounded one.
3. The cached entry skips validation too. When would that be wrong? (Think: the schema changes while
   the server runs, or one cache shared by two GraphQL objects.)
4. Cancellation stopped the slow query, but the fetcher that was running finished its 100 ms. What does
   graphql-java actually check, when, and what would a fetcher need to do to stop sooner?
