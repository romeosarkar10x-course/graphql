# Hints: 16.10 ops dashboard

1. Write each query in Explore first and check its legend, then add it as a panel. Rates come from counters
   (`_count`, `_sum`, `_total`) via `rate(...)`. A label you want to see must survive `sum by (...)`.
2. "Slowest" means time per call: the rate of `_sum` divided by the rate of `_count`, grouped the same way on
   both sides. `topk` keeps the panel readable. Use cost per client, not requests per client: one partner query
   can cost as much as a thousand page views.
3. An alert needs a condition that is false on a normal day. Compare against a baseline (`offset 1d`) or a fixed
   target from your capacity notes, and give it a duration. Each step of the runbook should name the panel or
   query to look at next.
