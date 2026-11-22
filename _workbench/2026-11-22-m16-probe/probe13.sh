#!/usr/bin/env bash
# Probe 13: game day seed 7, 2 minutes of traffic; the evidence a player would collect, metrics and traces.
source /course/tools/testlib.sh; source /course/modules/m16-production/contract/ops.sh
timeout 200 bash $OPS/gameday.sh --seed 7 --minutes 2 > /tmp/gd.log 2>&1 &
sleep 110
q() { echo "-- $1"; promq "$1" | jq -c '.[] | [.metric, (.value[1]|tonumber|.*100|floor/100)]' | sed 's/"__name__":"[^"]*",//'; }
q 'topk(5, sum by (graphql_field_coordinate) (rate(graphql_datafetcher_seconds_sum[1m])) / sum by (graphql_field_coordinate) (rate(graphql_datafetcher_seconds_count[1m])))'
q 'histogram_quantile(0.95, sum by (le, graphql_operation_name) (rate(graphql_request_seconds_bucket[1m])))'
q 'shelfie_subscriptions_open'
q 'sum(jvm_memory_used_bytes{area="heap"})'
q 'sum by (graphql_client_name) (rate(graphql_cost_sum[1m]))'
q 'sum by (graphql_client_name) (rate(graphql_request_seconds_count[1m]))'
echo "-- TraceQL: field spans over 150 ms"
traceql '{ name =~ "graphql field .*" && duration > 150ms }' 5 | jq -c '.[] | {rootTraceName, durationMs, spans: [.spanSet.spans[]? | .durationNanos]}' | head -5
traceql '{ name =~ "graphql field .*" && duration > 150ms } | by(name)' 5 2>/dev/null | head -c 600; echo
echo "-- TraceQL: slowest operations by client"
traceql '{ span.graphql.client.name = "partner-bulk" && name = "graphql query" }' 3 | jq -c '.[] | {rootTraceName, durationMs}' | head -3
sleep 1; kill %1 2>/dev/null; wait
