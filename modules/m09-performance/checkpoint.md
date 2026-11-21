# Checkpoint 9 quiz

Answer without notes, then check.

1. The dashboard selects 10 clubs, 20 members each, 5 reviews each, and each review's book. Naively,
   how many statements does `Review.book` alone cost, and why does the deepest field dominate?
2. What three things does a DataLoader do with the keys of one level?
3. Why does `members(first: 3)` need a registered loader rather than `@BatchMapping`?
4. Your chained loader works in a unit test and hangs in the Spring app. Why, and what is the fix?
5. One SQL statement for the whole page was slower than eight batched ones. Give one reason.
6. A reader sees another reader's profile, `X-Cache: HIT`. Which layer, and what should the response have said?
7. Why is APQ not a security feature, and what is?
8. ZGC on a 256 MB heap: what happened, and what does it teach about picking a configuration?

<details><summary>Answers</summary>

1. 1,000 (10 x 20 x 5): one per review. Each list multiplies every statement below it, so the last
   level has the most parents.
2. Collects them (no fetch on `load`), sends them in one batch on dispatch (deduped, in order), and
   remembers the answers for the rest of the request.
3. The key must include the argument, or two aliases with different `first` would share one answer.
   `@BatchMapping` has no field arguments.
4. Per-level dispatch sends a level's loaders once; a load made in `thenCompose` after that is never
   dispatched when the first batch is asynchronous. Enable chaining in the GraphQLContext
   (`setEnableDataLoaderChaining`) from an interceptor: Spring doesn't.
5. MULTISET became correlated JSON aggregation: the database builds and the app parses 1,000 small
   arrays, while the batched statements were flat index scans. Local round trips are cheap (about 0.1 ms).
6. The HTTP layer (Varnish served it). A personal response must be `Cache-Control: private, no-store`;
   Spring sends no Cache-Control, and Varnish then caches GETs for 120 s.
7. Anyone can register any document with its hash, so it doesn't limit what runs. An allow-list
   registered at build time (trusted documents) does.
8. Allocation stalls, then `OutOfMemoryError`, and every request failed; at 1 GB it was fine. Test at
   the heap you'll deploy with, read the logs behind surprising numbers, and repeat runs before
   trusting small differences.
</details>
