# Rubric: 18.1 Ten operations an agent can read (operations/ and REPORT.md)

Pass mark: 8 of 14.

| Points | Criterion |
| --- | --- |
| 3 | Descriptions: each says what comes back, where each variable's value comes from, and what `null` means |
| 2 | Descriptions name the defaults and limits that hide data (`upcomingOnly`, list sizes) |
| 2 | The two mutations: what changes, that problems come back in `userErrors`, idempotency or duplicates |
| 2 | Both runs scored (schema only; schema and operations), with the model and how it was prompted |
| 3 | Each failed or costly query explained: wrong field, hidden default, missing filter, over-fetching, cost |
| 2 | The riskiest query: why a server should not run it as written, and which policy (lab 18.3) would stop it |
