# Mobile clients: notes

Reference notes (grader only), from `bin/course mobile ./airplane.sh` on 2026-11-22 (Apollo Kotlin 5.2.0,
Kotlin 2.4.10, Gradle 9.9.1, JDK 21; `_workbench/2026-11-22-m12-probe/mobile-airplane*.txt`).

## The airplane-mode run

```
$ shelfie-mobile online
[network] The Midnight Readers (8 members), reading The Left Hand of Darkness: 4.2
--- airplane mode ON
$ shelfie-mobile online
[network] no club card: Failed to execute GraphQL http network request
$ shelfie-mobile offline
[cache] The Midnight Readers (8 members), reading The Left Hand of Darkness: 4.2
$ shelfie-mobile review b1 1 Read on a plane, not for me.
offline: review for b1 queued (1 in the outbox)
$ shelfie-mobile offline
[cache] The Midnight Readers (8 members), reading The Left Hand of Darkness: 4.2
--- airplane mode OFF
$ shelfie-mobile sync
synced: review r16 for b1, average now 3.67
[cache, after sync] The Midnight Readers (8 members), reading The Left Hand of Darkness: 3.67
$ shelfie-mobile online
[network] The Midnight Readers (8 members), reading The Left Hand of Darkness: 3.67
```

## What made the cache answer

`FetchPolicy.CacheOnly` read the card with no network; every command is a new JVM (an app restart), so the
data came from `shelfie.db`, the SQLite normalised cache (`SqlNormalizedCacheFactory("jdbc:sqlite:shelfie.db")`).
`NetworkOnly` failed in airplane mode with `ApolloNetworkException` (`response.exception`, not a thrown
exception). The mutation's response (`book { id averageRating }`) was normalised into the same record as the
card's `currentBook`, so "after sync" showed 3.67 without a refetch, because `extra.graphqls` declares
`@typePolicy(keyFields: "id")`. Without it Apollo Kotlin keys records by their path in the response; the same
run then showed `[cache, after sync] ... 4.2` while the server said 3.67 (`mobile-airplane-no-keyfields.txt`).

## The outbox

A line per queued `createReview` in `outbox.txt`, replayed in order by `sync`; a line whose replay fails at the
network stays queued, a `DUPLICATE` counts as synced (the earlier attempt reached the server, lab 12.8), any
other userError is dropped with a message. A real app would also: show the pending review at once (an
optimistic write that survives restarts, which Apollo's `optimisticUpdates` does not, as it lives in memory
and is rolled back on failure), sync in the background when connectivity returns, keep per-entity ordering,
handle conflicts (the book was deleted meanwhile), use an idempotency key instead of relying on DUPLICATE, and
tell the user what is still unsent.

## Android

The same Apollo Kotlin code runs on Android; the build adds the Android Gradle plugin and the SQLite cache
uses Android's SQLite driver (`SqlNormalizedCacheFactory` given a database name, not a JDBC URL; check the
factory's Android signature for your Apollo version). Airplane mode is real: `ConnectivityManager`
callbacks start the sync, ideally as a `WorkManager` job with a network constraint so it survives the app
being killed. Codegen and the cache are identical to this lab.
