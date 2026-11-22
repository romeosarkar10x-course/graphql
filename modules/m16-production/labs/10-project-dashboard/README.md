# Mini-project 16.10: The GraphQL ops dashboard and its runbook

Lecture: [16.1 Observability](../../lectures/16.1-observability.md) (and 16.5-16.6). Grading: auto + rubric. Needs
`bin/course ops`.

Build the dashboard you wished you had during the game day, and the runbook that goes with it.

**`dashboard.json`**: a Grafana dashboard (export it from Grafana: Share > Export > Save to file). It needs at
least these four panels. Their titles must contain these words, because the grader finds the panels by title:

| Title contains | Shows |
| --- | --- |
| "Top operations" | the busiest operations, by `graphql_operation_name` |
| "Error rate by code" | GraphQL errors per second by `code` |
| "Slowest fields" | average time per call by `graphql_field_coordinate` |
| "Cost per client" | cost per second by `graphql_client_name` |

Add what else helps: p95 by operation, heap, open subscriptions, a link to Tempo. Use the Prometheus data source
with uid `prometheus`. `$__rate_interval` is fine; the grader replaces it with `1m`.

**`RUNBOOK.md`**: at least three alerts. Each one has:

- its PromQL condition;
- the panel to open first;
- the diagnosis steps;
- the mitigation.

Write one for each failure you have seen in this module: a slow field or dependency, errors by code, an
expensive client, and a growing heap.

The grader:

1. starts Prometheus, Grafana and the reference Shelfie;
2. imports your dashboard;
3. sends 35 s of known traffic: web's club page most often, a partner's expensive query, bad input from Android,
   invalid documents from iOS, and the reviews service failing;
4. runs each panel's first query against Prometheus and checks which series comes out on top.

```bash
bin/course start m16/10
cd workspace/m16-production/10-project-dashboard
bin/course test m16/10
```

Build it live:

1. Run `COURSE_PUBLISH=3000 bin/course shell`, then `source` on `ops.sh` and run `ops_start; reviews_start`.
2. Start the reference app: `java -jar /course/modules/m16-production/contract/ops-app/target/shelfie-ops.jar &`.
   Build it once with `mvn -q package` in that folder.
3. Run `contract/gameday.sh` or `Traffic.java` for traffic.
4. Edit the dashboard at http://localhost:3000.

## Group variant

Each person builds the panels for one incident from game 16.8, then you merge them into one dashboard. Swap
runbooks with another group and run their steps against a game day seed they did not play.

## Done when

`bin/course test m16/10` passes and `RUNBOOK.md` and the dashboard pass the rubric (`bin/course grade m16/10`).
