#!/usr/bin/env bash
# Probe 2: why Tempo search is empty: does the app export, does Tempo receive?
source /course/tools/testlib.sh; source /course/modules/m16-production/contract/ops.sh
trap 'kill $APP 2>/dev/null; ops_stop' EXIT
ops_start tempo || exit 1
reviews_start || exit 1
java -jar /course/modules/m16-production/contract/ops-app/target/shelfie-ops.jar \
  --management.tracing.sampling.probability=1.0 \
  --management.opentelemetry.tracing.export.otlp.endpoint=http://localhost:4318/v1/traces \
  --management.otlp.metrics.export.enabled=false > /tmp/app.log 2>&1 & APP=$!
for i in $(seq 60); do curl -fs -o /dev/null localhost:4000/actuator/health && break; sleep 0.5; done
TID=4bf92f3577b34da6a3ce929d0e0e4736
for t in $(seq 1 2 60); do r=$(traceql '{}' | jq length); echo "tempo-up wait t+$t: $r"; sleep 2; done | tail -2
START=$(date +%s)
for i in 1 2 3; do curl -s -o /dev/null localhost:4000/graphql -H "traceparent: 00-$TID-00f067aa0ba902b$i-01" -H 'Content-Type: application/json' -d '{"query":"query ClubPage { club(id: \"c1\") { name readingList { title similar { title } } } }"}'; done
sleep 1
echo "== nonzero tempo metrics"; curl -s localhost:3200/metrics | grep -vE "^#|_bucket" | grep -E "live_store|ingest|kafka|distributor|querier|frontend|block_builder" | grep -vE " 0$" | head -40
echo "== app log export lines"; grep -iE 'otlp|export|span|warn|error' /tmp/app.log | head -10
echo "== tempo received"; curl -s localhost:3200/metrics | grep -E '^tempo_(distributor_spans_received_total|receiver_accepted_spans|live_store|ingester_traces)' | head -10
for t in $(seq 1 1 12); do sleep 1; echo "t+$t: byid=$(curl -s -o /dev/null -w '%{http_code}' localhost:3200/api/v2/traces/$TID) search=$(curl -s -G localhost:3200/api/search --data-urlencode q={} | jq -c '[.traces[]?.traceID] | length') ranged=$(traceql '{}' | jq length)"; done
echo "== search all"; curl -s -G localhost:3200/api/search --data-urlencode 'q={}' | head -c 600; echo
echo "== search tags"; curl -s localhost:3200/api/search/tags | head -c 400; echo
grep -iE 'level=(warn|error)' /tmp/ops/tempo.log | grep -v 'nil job' | head -5
