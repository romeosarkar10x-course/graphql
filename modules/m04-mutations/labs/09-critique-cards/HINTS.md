# Hints, game 4b

1. Look for what a client needs after the mutation: `deleteIssue` returns the `repository`, so a client can refresh its issue count; `addStar` returns the `starrable`, so the star count updates. That is the payload criterion.
2. Retry: is the operation naturally idempotent (starring twice leaves one star)? Does it offer concurrency control (`expectedHeadOid`: merge only if the head is still the commit you reviewed)? A toggle is the opposite of idempotent: retried, it undoes itself.
3. Errors: GitHub reports failures as top-level `errors` (each with a `type` such as `NOT_FOUND`), not in the payload; AniList returns the entity or an error. Neither has typed domain errors in the schema.
