# Shelfie on-call runbook

Dashboard: **Shelfie GraphQL ops** (`dashboard.json`, uid `shelfie-ops`). Traces: Grafana > Explore > Tempo.
Every alert below names the panel to open first and the query behind it.

## Alert: an operation is slow (p95 over 250 ms for 5 minutes)

Condition: `histogram_quantile(0.95, sum by (le, graphql_operation_name) (rate(graphql_request_seconds_bucket[5m]))) > 0.25`

1. Panel "p95 by operation": one operation or all of them? All of them points at the JVM, the host or a shared
   dependency; one points at a field it selects.
2. Panel "Slowest fields": which coordinate's average went up? Compare with the same panel a day earlier.
3. Tempo: `{ name = "graphql field <Type.field>" && duration > 200ms }`. Look under the span: an `http get` child
   means a dependency (reviews service: check its own health), no child means the fetcher itself.
4. Mitigate: if it is the reviews service, the circuit breaker should already be degrading `Book.reviews`
   (`REVIEWS_UNAVAILABLE` in "Error rate by code"); if not, check the breaker's settings. If it is a field of ours,
   roll back the last deploy.

## Alert: errors by code rise (any code over 1 per second for 5 minutes, or a new code)

Condition: `sum by (code) (rate(graphql_errors_total[5m])) > 1`

1. Panel "Error rate by code": which code? HTTP status stays 200 for most GraphQL errors, so HTTP dashboards stay
   green.
2. `REVIEWS_UNAVAILABLE`: the reviews dependency is failing or slow; pages still render. Check the reviews
   service; nothing to roll back here unless it started with our deploy.
3. `BAD_USER_INPUT` or `ValidationError`: a client sends bad requests. Split by `graphql_client_name`
   (`sum by (code, graphql_client_name) (rate(graphql_errors_total[5m]))`); a new client release is the usual
   cause. Tell its owners, with the operation name.
4. `INTERNAL_ERROR`: ours. Find a trace with an error span, roll back if it began with a deploy.

## Alert: one client costs too much (cost per second over 10x its usual)

Condition: `sum by (graphql_client_name) (rate(graphql_cost_sum[5m])) > 10 * sum by (graphql_client_name) (rate(graphql_cost_sum[5m] offset 1d))`

1. Panel "Cost per client": which client? Panel "Top operations": which of its operations?
2. If it is a partner looping an expensive query, apply its cost budget (Module 10.6) and contact the partner's
   technical contact with the operation name and the cost per request (`extensions.cost`).
3. Scale if the cost is legitimate: the autoscaler follows cost per pod; check it is not at `maxReplicas`.

## Alert: heap keeps growing (heap used up 50% in 30 minutes, traffic flat)

Condition: `sum(jvm_memory_used_bytes{area="heap"}) > 1.5 * sum(jvm_memory_used_bytes{area="heap"} offset 30m)`

1. Panel "Heap used and open subscriptions": does the slope match open subscriptions or connections?
2. Group open subscriptions by client; a client whose count only grows has subscriptions that are never released.
3. Mitigate: restart replicas one at a time (the rolling deploy of lab 16.4 drops no messages); fix the release
   path, then add the per-client subscriptions panel to the dashboard.

## After every incident

Write it up as in game 16.8: symptom, query, cause, mitigation, fix, and the alert that would have caught it
sooner. Add that alert here.
