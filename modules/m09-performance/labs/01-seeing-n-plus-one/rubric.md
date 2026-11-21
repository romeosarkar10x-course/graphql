# Rubric, lab 9.1 notes (pass: 7 of 10)

| Points | Criterion |
|---|---|
| 3 | All three predictions are written (wrong predictions are fine) and all three measured counts match the app's (1,250, 3, 22). |
| 3 | Each count has a formula that reproduces it from the fan-outs: e.g. 1 + 3 + 3x2 + 3x2x2 = 22, and the dashboard's 1 + 10 x 5 - 1 + 200 + 1,000, including why club 10 contributes one statement fewer. |
| 2 | The general rule: each list multiplies the statements of everything below it, so the deepest field dominates; to-one fields add one statement per parent. |
| 2 | Where the time goes: a measured latency for the dashboard and the point that the cost is round trips, not slow SQL (every statement uses an index), so a remote database makes it worse. |
