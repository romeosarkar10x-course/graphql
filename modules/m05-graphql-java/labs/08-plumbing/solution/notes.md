# Lab 5.8 notes

## Throughput (`java -jar target/shelfie-plumbing.jar`)

Course image, 2026-11-21, 20,000 requests after 2,000 warm-up:

| | req/s | parses |
|---|---|---|
| no cache | 55,621 | 20,000 |
| Caffeine cache | 266,598 | 0 (the one parse happened in warm-up) |

About 4.8x: for a cheap query, parse and validate dominate.

## Questions

1. Variables are inputs to execution, not to the document: the same parsed and validated document runs
   with any values, so keying on them would only lower the hit rate. If clients inline values
   (`club(id: "c1")`, `club(id: "c2")`, ...) every request is a new key: the hit rate drops towards zero
   and the cache fills with one-off entries. That is one more reason for variables, and later for
   persisted queries (Module 9).
2. The key is attacker-controlled text. An unbounded cache lets anyone send millions of unique documents
   (vary a comment, an alias or a literal) and hold each parsed AST in memory forever: a memory
   exhaustion attack that costs the attacker one small request per entry. Bounded size with eviction
   caps the damage at the bound.
3. Validation is against a schema. If the schema is hot-reloaded, or one provider instance is shared by
   two GraphQL objects with different schemas, a cached "valid" document may no longer be valid and
   would execute without errors being reported up front. The fix is one cache per schema (or the schema
   version in the key) and dropping the cache on reload.
4. `ExecutionInput.cancel()` only sets a flag. The engine checks it between steps (EngineRunningState
   .throwIfCancelled, called from ExecutionStrategy as it dispatches fields) and raises
   AbortExecutionException, "Execution has been asked to be cancelled" (probe:
   `_workbench/2026-11-21-m05-probe/cancellation.txt`). A running fetcher is never interrupted, so
   `slowTitle` finished its sleep. To stop sooner the fetcher must cooperate: be async and cancel its own
   future or HTTP call, or pass a deadline down
   (JDBC query timeout, HTTP client timeout) so the backend call itself gives up.
