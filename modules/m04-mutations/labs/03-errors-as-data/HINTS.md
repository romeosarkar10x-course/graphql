# Hints, lab 4.3

1. A domain failure is **data**: `data.createReview.userErrors` is a non-empty list and the top-level `errors` is absent. The starter never selects `userErrors`, so it cannot know.
2. For the union, select `__typename` plus one inline fragment per member: `... on ValidationFailed { problems { field message } }`. Without `__typename` the client cannot tell which member it got.
3. `field` is a list (`["input", "rating"]`); `String.join(".", ...)` over its items gives `input.rating`. In the union style the same information is under `problems`, not `userErrors`.
