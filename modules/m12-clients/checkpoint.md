# Checkpoint 12

`bin/course checkpoint 12` checks labs 12.1 to 12.5, 12.8 and mini-project 12.9, then tags `checkpoint-12`.
Answer these first, without looking.

1. A response has `data.book` and an error on `["book", "recommendations"]`. What should a client do with it?
2. `documentName("clubDigest")` in Spring's client: what does it send, and what does it not send?
3. Why can DGS codegen's generated requests hurt a server's document cache?
4. SmallRye's typesafe client sent `$id: ID` for `club(id: ID!)`. Why is that refused, and what fixes it?
5. A mutation returns `review { id }` only. Which cached queries does it update, and why?
6. After `createReview`, a cached `reviews(last: 5)` list is stale. Name two ways to fix it.
7. Why must a client not retry a non-idempotent mutation blindly, and what makes a replay safe?
8. Why did a "30% failure" Toxiproxy toxic hit only 1 request in 100 through a keep-alive client?

<details><summary>Answers</summary>

1. Use `data.book` (it is valid), treat `recommendations` as failed (null, with the error's message), and show
   the rest of the page. A typed `field()` call that demands error-free data throws, carrying the partial data.
2. It loads `graphql-documents/clubDigest.graphql` as the document. It does not send an `operationName`: call
   `.operationName(...)` if interceptors or server logs need it.
3. It inlines arguments as literals, so every argument value is a different document text: parsed-document
   caches and persisted-document stores miss for each value.
4. A nullable variable may not be used for a non-null argument (All Variable Usages Are Allowed). `@NonNull` on
   the parameter declares `ID!`.
5. Only the `Review` record itself: the book is not in the response, so no cached book or list changes. The
   mutation must select what it changes, with `__typename` and `id`.
6. Refetch the query; write the list back with the new reference (an update function); evict the list field
   so the next read misses; or return the updated list from the mutation.
7. A lost response may mean the server stored it: a blind retry duplicates. An idempotency key (or a server rule
   that detects the repeat, like Shelfie's `DUPLICATE`) lets the client recognise the replay.
8. Toxiproxy decides per connection; the keep-alive client reused one healthy connection for all calls.

</details>
