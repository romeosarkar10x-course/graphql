# Incident report: game day, seed 7

Two minutes of traffic, investigated from Grafana's Explore (Prometheus and Tempo) only. Numbers are from the
reference run (`_workbench/2026-11-22-m16-probe/probe13.txt`).

## Timeline

- 0:00 traffic starts; book operations are slow from the first minute.
- 0:20 a burst of expensive requests starts (the game's abusive client begins 20 s in).
- 1:50 all three identified from the queries below (one reading of each, taken at 1:50).

## Incident 1: the slow field

- Symptom: p95 by operation, `histogram_quantile(0.95, sum by (le, graphql_operation_name)
  (rate(graphql_request_seconds_bucket[1m])))`, showed `BookPage` at 0.35 s and `BookScreen` at 0.33 s while
  every other operation stayed in single-digit milliseconds. Both operations start with `Query.book`.
- Evidence: average time per field, `sum by (graphql_field_coordinate) (rate(graphql_datafetcher_seconds_sum[1m]))
  / sum by (graphql_field_coordinate) (rate(graphql_datafetcher_seconds_count[1m]))`: `Query.book` 0.24 s,
  `Book.similar` 0.03 s (its usual 30 ms), everything else near 0. In Tempo, `{ name =~ "graphql field .*" &&
  duration > 150ms }` returned only `graphql field Query.book` spans, 221 to 271 ms each, with no child span
  (no HTTP call to the reviews service under it), so the time is spent inside the fetcher itself.
- Cause: `Query.book` itself is slow, about 250 ms per call, on every call, while `Query.books` (same data) is not.
  A lookup by id should be a map access.
- Mitigation now: none needed to keep pages up (no errors), but a response cache for `BookPage` would hide it.
  Fix later: profile the fetcher; it is a code change, not load.

## Incident 2: the memory leak

- Symptom: `sum(jvm_memory_used_bytes{area="heap"})` read 271 MB at 1:50 with modest traffic. One reading proves
  little; graphed over the game in Grafana it is the line to watch next to open subscriptions.
- Evidence: `shelfie_subscriptions_open` by client: android 1, ios 1, partner-bulk 1, **web 48**. Every client
  opens and closes one subscription every two seconds (their WebSocket traffic looks the same), so web's count
  should also hover around 1. It only goes up: web's subscriptions are never released when they end.
- Cause: subscriptions from the `web` client keep their state after completion (the gauge counts them; the
  heap holds them).
- Mitigation now: restart replicas on a schedule, or limit subscriptions per client. Fix later: find what keeps a
  reference after `complete` for that client's connection path; add an alert on
  `shelfie_subscriptions_open` growing for 10 minutes with flat WebSocket connections.

## Incident 3: the abusive client

- Symptom: requests per second by client, `sum by (graphql_client_name) (rate(graphql_request_seconds_count[1m]))`:
  partner-bulk 86.8 per second against 5 to 8 for the others, from 20 s into the game.
- Evidence: cost per second by client, `sum by (graphql_client_name) (rate(graphql_cost_sum[1m]))`: partner-bulk
  156,797, web 171, android 55, ios 46. Its operation is `Recommendations` (p95 0.11 s): `books(first: 10)` with
  `similar` nested twice and reviews at the bottom, 1,871 cost per request by the rule. In Tempo,
  `{ span.graphql.client.name = "partner-bulk" && name = "graphql query" }` showed traces of about 100 ms each.
- Cause: a partner integration running an expensive recommendations query in a tight loop.
- Mitigation now: a per-client cost budget (Module 10.6) for partner-bulk; contact the partner. Fix later: a
  persisted, cheaper recommendations operation, and rate limits by cost per client rather than by requests.

## What would have caught each one sooner (alerts, dashboards)

- p95 per operation over 250 ms for 5 minutes (incident 1 would have fired within the first minutes).
- Heap after GC growing for 15 minutes, together with open subscriptions per client against connections.
- Cost per second per client above 10x its seven-day median (incident 3 fires in seconds).
All three are panels on the ops dashboard of lab 16.10.
