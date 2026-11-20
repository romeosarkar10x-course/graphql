# Hints, lab 3.7

1. `sealed interface FeedItem { ... }` and the records `implements FeedItem` in the same file: that is the whole of TODO 1.
2. `String type = switch (item) { case ReviewPosted r -> "ReviewPosted"; ... };` A switch *expression* over a sealed type must be exhaustive; with all four cases and no `default`, a fifth record makes it fail to compile. `item` must have the static type `FeedItem` (cast `env.getObject()`), not `Object`, or the compiler cannot know the set is closed.
3. The c1 feed with the starter: what does graphql-java do when the resolver names `MeetupScheduled` for a `BookFinished` record? Each field falls to the default fetcher on the wrong record. That silent wrongness is what the sealed switch prevents.
