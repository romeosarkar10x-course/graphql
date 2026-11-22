# Game 12.11: Same screen, three clients

Lectures: [12.2](../../lectures/12.2-spring-clients.md), [12.6](../../lectures/12.6-web-clients.md),
[12.7](../../lectures/12.7-mobile-clients.md). Grading: rubric. Needs what labs 12.2, 12.6 and 12.7 need.

One feature, three clients, one page of honest comparison. **The feature:** the club card for `c1` (club
name and member count, current book with its average rating) and an "add review" action on the current book,
after which the card shows Shelfie's new average **without refetching the card**.

| Client | Starting point |
|---|---|
| Spring's `HttpSyncGraphQlClient` (a Java service or CLI) | lab 12.2's digest; it has no cache, so "without refetching" needs a decision of your own |
| Apollo Kotlin (JVM, SQLite cache) | lab 12.7 |
| A web client: Apollo Client or Relay, rendered in jsdom | lab 12.6 |

Then fill in `comparison.md` (one page): lines you wrote per client, what each generated or did for you,
how each handled the mutation's result (normalised cache, manual update, or refetch), what broke on the way,
the build and tooling cost, and which you would pick for a Shelfie web app, an Android app and a backend that
calls Shelfie. Measure, do not guess: request counts from `/_course/stats` or `contract/Capture.java`, render
counts from the web harness, line counts from `wc -l`.

## Solo

Build all three yourself (reuse your lab code), then write the page.

## Group

Two or three players, one client each, same feature, one hour on the clock. Agree on the feature's exact
behaviour first (what the card shows, what "add review" sends). Then demo side by side against one Shelfie:
everyone adds a review on the same book in turn, and every client must show the same new average. Each player
writes their column of `comparison.md`; together write the last section, the picks, and argue them out. The
grader scores the shared page, plus a point per column backed by real numbers.

## Done when

`comparison.md` passes the rubric (`bin/course grade m12/11`).
