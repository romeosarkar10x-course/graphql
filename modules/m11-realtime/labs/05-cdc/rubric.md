# Rubric, lab 11.5 notes (pass: 7 of 10)

| Points | Criterion |
|---|---|
| 4 | The experiment from real runs: requests, delay (median and p95) and server CPU for idle, polling and subscriptions, in a busy and a quiet room, and what scales with what (clients × poll rate vs clients × events). Invented numbers score 0. |
| 3 | What CDC changed: every writer's rows become events, the row is the event (no "saved but not published" gap), the cost (a replication slot holding WAL, lag set by the engine's poll interval, what happens to events while the server is down). |
| 3 | Live queries, webhooks and CDC compared with subscriptions: who pushes, who is the client, what each is good for in Shelfie. |
