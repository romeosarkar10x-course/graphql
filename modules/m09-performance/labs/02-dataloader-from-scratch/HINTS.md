# Hints, lab 9.2

1. Two maps do most of the work: one from key to future for everything ever loaded (the memo), and one
   for keys not sent yet. A `LinkedHashMap` keeps the queue in first-request order, and putting a key
   that's already there doesn't add it twice.
2. In `dispatch()`, copy the queue and clear it *before* calling the batch function. Then a load made
   while a batch completes lands in the new, empty queue, and the batch function may even complete
   synchronously without breaking anything. Wrap the call in `try`, so a batch function that throws
   becomes a failed future.
3. Complete the futures in `whenComplete((results, error) -> ...)`: decide first whether the whole batch
   failed (an error, `null`, or the wrong size), then go key by key. To not remember a failure, take
   the key out of the memo before failing its future, but only if the memo still holds that same future.
