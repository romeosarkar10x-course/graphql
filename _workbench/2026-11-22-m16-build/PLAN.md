# Module 16 build plan (working notes)

Contract: modules/m16-production/contract/
- ReviewsService.java (:4100) dependency; ops.sh (Tempo/Prometheus/Grafana from .cache/ops, need "ops")
- ops-app/ = reference Shelfie with every feature, one class per feature:
  Observability.java (16.1) UsageReporter.java (16.2) Live + Shutdown (16.4) ReviewsClient resilience (16.5) CostMetric (16.6)
- labs assemble from ops-app: starter = minus the feature, solution = with it.

Labs: 01-observability(both) 02-field-usage(auto) 03-governance(both) 04-deployment(both) 05-reliability(auto)
06-capacity(both) 07-public-product(both) 08-incident-game-day(game,both) 09-governance-council(game,rubric)
10-project-dashboard(project,both: dashboard.json + RUNBOOK.md)
Checkpoint: 01 04 05 10.

Findings so far: see _workbench/2026-11-22-m16-probe/FINDINGS.md (to write).
- Tempo 3.1.1 needs live_store/block_builder/backend_scheduler paths (default /var/tempo).
- Trace by id at once (export schedule-delay 500ms); TraceQL search only ~20-30 s later.
- Default spans: "graphql query", "graphql field <name>" (no parent type), path attr; dataloader spans; metrics graphql_request_seconds{graphql_operation_type,graphql_outcome}, graphql_datafetcher_seconds{graphql_field_name}, no op name.
- Hive: report v2 schema, X-Usage-API-Version: 2, Bearer token, https://app.graphql-hive.com/usage, headers x-graphql-client-name / graphql-client-name.
