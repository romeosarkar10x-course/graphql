# Lab 5.8 — Context, cache, cancel

**Lecture:** [5.8 Request plumbing](../../lectures/5.8-request-plumbing.md) · **Grading:** auto + rubric · **Needs:** nothing

A Maven project (`mvn -q package`, then `java -jar target/shelfie-plumbing.jar`). The test harness calls
`dev.shelfie.plumbing.Plumbing`'s public methods; keep their signatures.

| TODO | Done when |
|---|---|
| A `PreparsedDocumentProvider` backed by Caffeine, plugged in when `graphQL(true)` | 100 identical requests parse once; different variables, same document: once; `graphQL(false)` parses every time |
| Bound the cache | `maximumSize` (or an expiry) in your code |
| `Club.viewerIsMember` from the `viewer` in `GraphQLContext` (`Plumbing.input` puts it there) | true / false per club, null without a viewer |
| `executeWithin(graphQL, input, budget)` | a 1-second query comes back after about 250 ms with a "cancelled" error, and fewer than 10 of its slow fetchers started |

`Main` prints requests per second with and without the cache, and what cancellation did. Copy the
numbers into `notes.md`.

## Done when

`bin/course test m05/08` is green and `bin/course grade m05/08` gives 7/10 or more.
