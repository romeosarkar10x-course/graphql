# Spot the bug

Reference write-up (grader only).

| Bug | Symptom you saw (or would see in production) | Root cause | Fix |
|---|---|---|---|
| B1 | Fine alone; under load every parallel stream and async task in the JVM stalls, ratings latency climbs | `supplyAsync` without an executor runs a 50 ms blocking call on `ForkJoinPool.commonPool()` (cores - 1 threads, shared) | Pass a dedicated executor (virtual threads) |
| B2 | Labels like "Slow Burn MysteriesThe Midnight Readers (8)" or wrong counts, only under concurrency | One static `StringBuilder` shared by every request thread | Build the string locally (or `String.format`) |
| B3 | `ClassCastException: Data$Review cannot be cast to Map`, `book` null with an error | The fetcher assumed the source was a `Map`; the parent returned records | `env.<Data.Review>getSource()`; know what the parent returns |
| B4 | After anyone views "members by name", every club page lists members alphabetically; join order lost | `sort` in place on the store's own mutable list (the in-memory version of leaking a JPA entity and mutating it) | Sort a copy / stream; return immutable views |
| B5 | `books(first: null)` NPE on unboxing; `first: 50` `IndexOutOfBoundsException` | Explicit `null` overrides the default; `subList` trusts the client's number | Handle null, clamp with `limit` |
| B6 | `club(id: "c1")` comes back `null` with "non-null field owner returned null" | Looked up the owner by the club's id; null in a `User!` field propagates to `club` | Use `ownerId`; `orElseThrow` so the bug would have been loud |
| B7 | `viewerIsMember` always null in production, true in a single-threaded test | `ThreadLocal` set on the request thread, read on a pool thread | Read the viewer from `GraphQLContext` |
| B8 | `clubs { currentBook }` hangs forever once there are more clubs than pool threads | Outer task on a 2-thread pool `join()`s an inner task queued on the same pool: thread starvation deadlock | Return the inner future (compose), never block inside the pool |

## Which would a code review catch, which only a load test?

Review catches B3, B5, B6 and B4 if the reviewer knows the store returns mutable lists: they are wrong
on every call. B1, B2, B7 and B8 pass every single-request test and only appear with concurrency or
with more data than threads: they need a load test, a thread-dump habit, or a rule ("no `join()` in
fetchers", "always pass an executor", "no static mutable state", "no ThreadLocal for request data")
that a linter or ArchUnit test can enforce.
