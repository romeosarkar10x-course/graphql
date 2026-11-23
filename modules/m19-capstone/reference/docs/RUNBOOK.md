# Toolshed runbook

The stack: Hive Router (:4000), inventory (:4001), lending (:4002), PostgreSQL. Dashboard: "Toolshed" in
Grafana (`ops/dashboard.json`). Traces: Tempo, service names `router`, `inventory`, `lending`. Logs:
`/tmp/toolshed-*.log` when started with `scripts/run.sh`.

## Alert: lending is down

**Condition:** `up{job=~"capstone.*", instance="localhost:4002"} == 0` for 1 minute, or
`sum(rate(graphql_request_seconds_count{application="lending"}[5m])) == 0` while inventory has traffic.

**What members see:** the catalogue loads, but every badge says "ask at the desk" (`available: null`); borrowing
and "my loans" fail.

**Do:** check `/tmp/toolshed-lending.log` for the last exception; check PostgreSQL (`pg_isready`). Restart
lending. Note: lending reloads its seed at start (ADR 0004) in this version, so a restart loses loans; until
migrations exist, restart only after copying `toolshed_lending.loan` (`\copy` to a file).

## Alert: slow catalogue

**Condition:** `histogram_quantile(0.95, sum by (le, application) (rate(graphql_request_seconds_bucket[5m]))) > 0.25`
for 5 minutes.

**Do:** open the slowest trace in Tempo for the last 15 minutes (`{ resource.service.name = "router" } |
duration > 250ms`). If the time is in one subgraph's `graphql query` span, look at the "SQL statements per
request" panel for that subgraph; if the time is between spans, look at the router's CPU and the host.

## Alert: statements per request jumped (an N+1 shipped)

**Condition:** `sum by (application) (rate(toolshed_db_statements_total[5m])) / sum by (application) (rate(graphql_request_seconds_count[5m])) > 8`
(normal: 3 to 5 in inventory, 1 to 3 in lending).

**Do:** this follows a deploy. Find the operation in the traces whose `graphql query` span has many child
JDBC spans; the field whose resolver is not a `@BatchMapping` or DataLoader is the cause. Roll back, then add
the case to `InventoryGraphQlTest.statementsDoNotGrowWithThePage` before fixing.

## Alert: field errors other than FORBIDDEN and UNAUTHORIZED

**Condition:** `sum(rate(graphql_datafetcher_seconds_count{graphql_outcome="ERROR"}[5m])) > 1` and the
"Field errors by field" panel shows a field other than `email`, `member`, `currentLoan`, `overdueLoans`.

**Do:** an INTERNAL_ERROR hides the message from clients; the subgraph's log has the exception with the
request's trace id. Search Tempo by that id.

## Symptom: "this tool is out" but it is on the shelf

The open loan was never returned in the system. A librarian returns it (`ReturnTool` as admin). If the tool is
not in `toolshed_lending.tool_stock` at all, borrowing it says `ToolNotFound`: find such ids with

```sql
SELECT id FROM toolshed_inventory.tool EXCEPT SELECT tool_id FROM toolshed_lending.tool_stock;
```

## Symptom: the live badge stops changing

Subscriptions come from lending's memory (ADR 0006). After a lending restart, open pages must re-subscribe
(reload). Check `/tmp/toolshed-router.log` for subscription errors. If two lending instances were started by
mistake, stop one: events from the other never reach its subscribers.

## Change: a client needs a new operation

1. Add the file to `operations/`, run `scripts/persist.sh`, commit both `router/persisted-documents.json` and
   `tests/ids.env`. 2. Restart the router (it reads the manifest at start, `watch: false`). Until then the
   new operation is `PERSISTED_DOCUMENT_NOT_FOUND`.

## Change: a cost limit refuses a real screen

The router's log has `rejecting operation: estimated cost exceeds configured max cost` with the operation name
and estimate. Check `@listSize` on the fields involved before raising `demand_control.operation_cost.max` in
`router/router.yaml`; a wrong `assumedSize` is the usual cause.
