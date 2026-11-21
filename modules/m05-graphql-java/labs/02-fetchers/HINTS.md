# Hints, lab 5.2

1. `env.getExecutionStepInfo().getPath()` is a `ResultPath` like `/club/members[1]/handle`. Its
   `getParent()`, `isListSegment()`, `getSegmentIndex()` and `getSegmentName()` give you every piece of a
   line; count the name segments (not the index segments) for the depth.
2. A fetcher can return a `DataFetcherResult` instead of a plain value. Besides `data`, it carries
   `errors` and a `localContext`, which graphql-java hands to the children of that field (and only them)
   as `env.getLocalContext()`.
3. `GraphqlErrorBuilder.newError(env)` fills `locations` and `path` from the environment; override the
   path with `env.getExecutionStepInfo().getPath().segment(i)` to point at one list entry.
