# Rubric, lab 12.8 notes (pass: 8 of 12)

| Points | Criterion |
|---|---|
| 3 | Real before/after numbers from the test, the plain rate explained from the toxics, and the per-connection point. Invented numbers score 0. |
| 3 | Which operations are safe to retry, how the lost-response case was told apart, and idempotency keys as the general fix. |
| 3 | Breaker settings with a reason, the decorator order, and what retries without a breaker do (retry storms, jitter). |
| 3 | APQ from the client: hash first, PersistedQueryNotFound after a restart, and why it is not a transport failure. |
