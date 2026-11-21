# Lab 8.7 — The page that survives

**Lecture:** [8.6 Partial results on purpose](../../lectures/8.6-partial-results.md) · **Grading:** auto · **Needs:** nothing

A club page has a name, members and recent reviews. The reviews live in another service, which is
sometimes down and sometimes only half there. The starter lets one bad dependency take the whole page
with it: `recentReviews` is `[Review!]!`, so its exception nulls `club`. It also loads the club again for
every member, just to answer `isOwner`. Fix both with one graphql-java type: `DataFetcherResult`, which
lets a fetcher return **data, errors and a local context** together.

`Services.java` holds the back ends: `ClubService` (counts `load` calls) and `ReviewsService` with three
modes, `UP`, `DOWN` (throws `ReviewsUnavailableException`) and `PARTIAL` (a `Batch` whose
`failedIds` are `r2` and `r4`). Don't edit it; the test uses its own copy. Run the page with
`bin/course java ClubPage.java DOWN`.

## Do

In `ClubPage.graphQL(...)`:

1. **DOWN**: `recentReviews` returns an empty list *and* one error at its own path, with message
   `Reviews are unavailable right now` (no host names) and `extensions.code: REVIEWS_UNAVAILABLE`.
   Name, members and everything else render.
2. **PARTIAL**: return the reviews that loaded, plus **one** error at `[club, recentReviews]` with
   `extensions.code: REVIEWS_INCOMPLETE` and `extensions.missing: ["r2", "r4"]`. One error, not one per
   review: error paths should be unique (RFC #1183, lecture 8.6).
3. **localContext**: `Member.isOwner` must not call `clubs.load(...)`. The club that `members` and
   `recentReviews` already have travels down to `Member` (and to `Review.author`, also a `Member`) as the
   local context. One club load per request.

## Done when

`bin/course test m08/07` is green (15 checks).
