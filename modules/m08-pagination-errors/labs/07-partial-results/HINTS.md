# Hints, lab 8.7

1. `DataFetcherResult.newResult().data(list).error(error).localContext(obj).build()`. A fetcher may
   return this instead of the plain value, and graphql-java unwraps it: the data becomes the field's
   value, the errors join the response, and the local context is visible to every field below. Build
   errors with `GraphqlErrorBuilder.newError(env)` so the path and location are filled in.
2. Catch `Services.ReviewsUnavailableException` only around the `reviews.recent(...)` call. Catching
   everything would also hide your own bugs as "reviews are unavailable". `Map.of("code", ..., "missing", batch.failedIds())`
   gives the extensions.
3. A local context reaches only the subtree of the field that set it. `isOwner` sits under
   `members` and under `recentReviews { author }`, so both fetchers must set it. Read it with
   `env.getLocalContext()`, which is the `Services.Club` you put there, and compare its `ownerId()`.
