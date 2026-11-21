# Hints, game 8a

1. The starter's bug: `offset: 20` means "skip whatever the first 20 are now". After 3 new books arrive
   on top, the last 3 you showed are at positions 20 to 22 again. Stop counting positions and remember
   *where you were*.
2. `feed(first: 20, after: $cursor)` returns `edges { node { id } }` and `pageInfo { hasNextPage endCursor }`.
   The cursor encodes the last id you got, and the server answers "the next 20 *older* than that".
   New books are newer, so they can't push anything into your next page.
3. Loop: request, append the edges, `after = pageInfo.endCursor`, stop at `count` or when `hasNextPage`
   is false. Ask for `min(20, count - shown.size())` so the last request doesn't fetch more than
   you need. Deletes need no special code: the next page starts after the cursor whether or not that
   book still exists.
