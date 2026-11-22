# Rubric: 16.10 ops dashboard (dashboard.json, RUNBOOK.md)

Pass mark: 6 of 10.

| Points | Criterion |
| --- | --- |
| 2 | Panels have units and readable legends, and descriptions that say what normal looks like |
| 2 | The dashboard goes beyond the four required panels with something an on-call person needs (latency, heap, subscriptions, links to traces) |
| 3 | Each runbook alert has a condition that would be false on a normal day, the panel to open first, and diagnosis steps that narrow down |
| 2 | Mitigations are specific to this service (breaker, cost budget, rolling restart, rollback), not "investigate" |
| 1 | The runbook says how an incident feeds back into a new alert |
