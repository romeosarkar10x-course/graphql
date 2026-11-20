# Hints, lab 3.4

1. Walk up from the failing field. If it is nullable, it becomes null and nothing else changes. If it is non-null, its parent becomes null instead; repeat. A list item counts as a step: `[User!]` means the item is non-null, so the step goes to the list.
2. The root of the response is `data`. If the walk reaches a non-null root field, `data` itself is null. Two root fields fail independently because each is its own step under `data`.
3. For the fix, decide per field: does a null here mean "unknown, show the rest" or "this object makes no sense without it"? A rating is the first kind. One bad member could be handled at the list item (`[User]!`) or at the field (`name: String`): which one hides less, and which one is easier for a client?
