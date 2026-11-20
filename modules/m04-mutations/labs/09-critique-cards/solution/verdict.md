# Verdict (reference)

## Worst card, rewritten

```graphql
setFavourite(input: SetFavouriteInput!): SetFavouritePayload!
input FavouriteTarget @oneOf { animeId: ID  mangaId: ID  characterId: ID  staffId: ID  studioId: ID }
input SetFavouriteInput { target: FavouriteTarget!  favourite: Boolean! }
type SetFavouritePayload { favourite: Boolean  target: Favouritable  userErrors: [UserError!]! }
```

Why this one: `ToggleFavourite` is unsafe to retry by construction (a retry flips the state back), its
"exactly one id" rule lives only in documentation, and the response does not say the new state.
Setting an absolute value makes retries harmless; `@oneOf` makes the rule a schema rule.

## Best card, and what to copy

`mergePullRequest`: one input, a typed enum with a sensible default, and `expectedHeadOid`, a
compare-and-set that prevents merging a commit nobody reviewed. Copy the idea for any write that acts
on something that can change under you (lab 4.4's `expectedVersion`).
