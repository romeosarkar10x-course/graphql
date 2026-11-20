# Hints, game 4a

1. Two separate bugs. Lost updates: `balances.put(u, balances.get(u) + x)` from two threads loses one write, even on a `ConcurrentHashMap` (each call is atomic, the pair is not). Double effects: nothing remembers that `requestId` s1-17 already ran.
2. Remember answers, not just ids: a retry must get the *same payload* the first attempt got (including "not enough points"), or the client sees a different story. Store requestId → payload.
3. The check for "already answered" and the effect must happen as one atomic step: a duplicate that arrives while the first copy is still running must wait for it, not run in parallel. One lock around both is enough here; `ConcurrentHashMap.computeIfAbsent` alone is not, because a transfer touches two users.
