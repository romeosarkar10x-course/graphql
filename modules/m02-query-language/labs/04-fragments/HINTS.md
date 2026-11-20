# Hints, lab 2.4

1. Look for selection sets that appear more than once on the same type: the four-field user, the five-field book, the club with its owner. Each one is a fragment: `fragment UserCard on User { ... }`, spread with `...UserCard`.
2. A named fragment can be spread inside an inline fragment's place: `... on Book { id title ... }` can become `...BookSummary`, because the spread carries its own type condition. Fragments can spread other fragments.
3. If the data matches but the bytes do not, a field moved. The response lists fields in the order they are first selected, and a spread puts its fields where the spread is. Put each spread where the first of its fields used to be.
