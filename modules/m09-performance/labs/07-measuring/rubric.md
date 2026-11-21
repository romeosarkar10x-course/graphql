# Rubric, lab 9.7 report (pass: 7 of 10)

| Points | Criterion |
|---|---|
| 3 | Both tables filled from real runs with the same load and warm-up for before and after; the setup says what load, how long, and on what. Invented numbers score 0. |
| 2 | Compares percentiles, not averages, and says what the spread (p50 vs p99) means at this load (no queueing yet). |
| 2 | Explains the Gatling vs Micrometer difference: client side vs server side, the measured phase vs a sliding window that includes the warm-up; and that server percentiles can't be averaged across instances. |
| 2 | Where the time goes, from the flame graph or `jfr view hot-methods`, with at least one concrete frame before and after. |
| 1 | Says what to measure next and why. |
