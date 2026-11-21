# Rubric, lab 11.3 notes (pass: 7 of 10)

| Points | Criterion |
|---|---|
| 4 | The flood experiment before and after from real runs: how many events the fast and the slow client got, when the slow client's last event arrived, and where the backlog lived. Invented numbers score 0. |
| 3 | Why each operator: the per-subscription filter, windowing per author (and why `sample` alone is wrong), `directBestEffort` vs a buffer, `doFinally` vs `doOnComplete`, `takeUntilOther`. |
| 3 | When dropping events is right (typing, presence, cursors) and when it is wrong (chat messages, payments), and what a chat message stream should do for a slow client instead. |
