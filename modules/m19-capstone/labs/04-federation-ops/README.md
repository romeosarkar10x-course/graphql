# m19/04 Stage 4: federation, observability and CI

About 5 hours. Two or more subgraphs behind a router, a stack you can see into, and CI that would stop a bad
change.

## Do

1. **Federation** (Module 15): at least two subgraphs with an entity between them, composed with the course
   composer (`source /course/modules/m15-federation/contract/fed.sh; compose supergraph.yaml out.graphql`),
   behind Apollo Router or Hive Router (`bin/course fed`). `api` is now the router.
2. **Tracing** (Module 16): every service and the router export OTLP to `http://localhost:4318/v1/traces` and
   propagate `traceparent`. Add the `crossSubgraph` probe: one operation that touches two subgraphs.
3. **Metrics and a dashboard:** `metrics` lists your services' Prometheus endpoints; `dashboard` is a Grafana
   dashboard JSON (data source uid `prometheus`) with at least four panels that mean something to an operator
   of your product.
4. **A runbook** (`docs/RUNBOOK.md`): at least three alerts, each with a PromQL condition and what to do.
5. **CI** (Module 13): a workflow with the gates: build and tests (unit, `GraphQlTester`, Testcontainers),
   the N+1 test, a schema-diff gate, PIT, and a step that fails when the composed supergraph or the trusted
   documents are stale. The checker reads it; it does not run it.

## Check

```bash
bin/course test m19/04     # also starts Tempo, Prometheus (scraping your "metrics") and Grafana
bin/course grade m19/04
```

## Done when

Green, and you can show one request's trace across your subgraphs and say what each dashboard panel is for.
