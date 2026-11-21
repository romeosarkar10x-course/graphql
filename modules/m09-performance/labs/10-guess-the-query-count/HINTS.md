# Hints, game 9b

1. In the naive app, every field below the root that isn't a column of its parent is one statement
   *per parent*. Multiply down the tree: 1 for the root, then the number of parents at each level.
2. Read the data rules again: some clubs have no current book, and every club has exactly 25 members.
   Look for a resolver that returns before it queries, and an argument asking for more rows than exist.
3. In the batched app it is one statement per loader per dispatch, not per parent. Root fields aren't
   batched there. Two loads at the same level for the same loader share a batch.
