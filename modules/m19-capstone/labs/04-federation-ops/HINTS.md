# Hints, stage 4

1. Compose by hand first (Module 15's `compose`), start the router on the result, and send one operation that
   needs both subgraphs. If the router answers but a field is null, look at the router's log for the subgraph
   call that failed: it is usually an entity resolver that does not take the whole list.
2. "Tracing: the trace should hold spans from at least two services": send a request with your own
   `traceparent` header and open the trace by id (`curl localhost:3200/api/v2/traces/<id>`); search lags by 20
   to 30 seconds, lookup by id does not. Each service needs a distinct `service.name` (Spring uses
   `spring.application.name`).
3. If dashboard panels are empty, run each panel's PromQL in Prometheus (`:9090`) after the suite: a metric
   name with a different suffix (`_total`, `_seconds_count`), a label your services do not set, or a panel
   over a metric only the router exports (the checker's Prometheus scrapes only your `metrics` URLs).
