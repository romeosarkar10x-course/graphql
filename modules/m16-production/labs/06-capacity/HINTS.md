# Hints: 16.6 capacity

1. The cost depends only on the document, the operation and the variables, so you can compute it before
   execution. graphql-java's `QueryTraverser` walks the selected fields. Its visitor environment knows each
   field's definition, its arguments and its parent environment, which is all you need to multiply.
2. A field's multiplier is the product of the sizes of the list fields above it, not including itself:
   `books(first: 2)` costs 1, and `books.title` costs 2. `instrumentExecutionResult` can return a new result with
   the extensions you add. Record the summary there too, with the client name from the GraphQL context.
3. `rate()` over a counter gives per-second values. `sum by (instance)` keeps one series per pod. For the
   target, run `HpaSim.java` with `--trace` at several values. Too close to capacity and new pods arrive late;
   too far below and you pay for idle pods, and the 300 s scale-down window keeps them longer.
