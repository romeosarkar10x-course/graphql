# Rubric, mini-project 4 (10 points)

| Criterion | Points | Full marks |
|---|---|---|
| Command design | 3 | One record per change, each self-contained (no reads of current state to reverse); conflict rules match "never overwrite a later change" |
| Concurrency | 1 | Token lookup, conflict check, apply and mark-used are atomic |
| Expiry and storage | 2 | A UI-driven expiry and durable storage in the same transaction as the change |
| Retrying undo | 2 | Sees that undo is not idempotent and replays the first answer |
| No-undo mutation | 2 | Names an external side effect and the alternative (confirm first, or delay) |

Pass: 7/10.
