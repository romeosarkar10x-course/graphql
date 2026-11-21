# Hints, game 5a

1. Write round 1 honestly before touching INTENT.md: the score is the point of the game, and the
   rubric only grades what you learned from the gaps.
2. Several intents depend on things that are not in the parent object: the viewer
   (`env.getGraphQlContext()`) and the club's meetups or the user's reviews (`Data`'s helpers).
3. `Data.meetupsOf(clubId, true)` already returns upcoming meetups in date order; `Data.reviewsBy`
   returns a user's reviews; `Data.REVIEWS` is every review. Comparators on `createdAt` do the rest.
