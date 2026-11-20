# Checkpoint 04 quiz

Answer without running anything, then check.

1. `mutation { a: renameClub(...) b: renameClub(...) }`: in what order do they run, and what makes a
   query's root fields actually run in parallel in graphql-java?
2. Name three things wrong with `addReview(bookId: ID!, userId: ID!, text: String!): Boolean`.
3. A client never selects `userErrors`. What does it see when a review is rejected?
4. 50 concurrent requests with one idempotency key: what decides which one creates the review?
5. `UPDATE ... WHERE id = ? AND version = ?` changed 0 rows. What do you return?
6. Which validation layer catches twelve spaces as a review body, and why not `@Size`?
7. Why does a multipart GraphQL endpoint need a custom header check?
8. Why is `ToggleFavourite` unsafe to retry, and what signature fixes it?

<details><summary>Answers</summary>

1. Document order, serially (a, then b). Fetchers that return `CompletableFuture`s running on another
   executor; blocking fetchers serialise even a query.
2. Positional arguments instead of one input; identity from the client (`userId`); a `Boolean` with no
   created object and no error channel (also: noun-ish name `text` vs `body` is minor).
3. `review: null` and no top-level error: it looks like success with missing data.
4. The database's unique constraint on the key, claimed in the same transaction as the insert.
5. A `CONFLICT` user error with the current version (after checking the row exists: else `NOT_FOUND`).
6. Bean Validation's `@NotBlank`: `@Size` counts characters, and spaces are characters.
7. Multipart POSTs are CORS simple requests: a foreign page can send them with the user's cookies and
   no preflight. A custom header forces a preflight.
8. A retry after a lost response flips the state back. `setFavourite(input: {target, favourite: Boolean!})`
   with an absolute value (and `@oneOf` for the target).
</details>
