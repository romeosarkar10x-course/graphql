#!/usr/bin/env bash
# Probe 1: Spring for GraphQL 2.1 observations -> Prometheus metrics and Tempo spans (defaults, no customisation).
source /course/tools/testlib.sh; source /course/modules/m16-production/contract/ops.sh
trap 'kill $APP 2>/dev/null; ops_stop' EXIT
ops_start || exit 1
reviews_start || exit 1
java -jar /course/modules/m16-production/contract/ops-app/target/shelfie-ops.jar \
  --management.endpoints.web.exposure.include=health,prometheus --management.tracing.sampling.probability=1.0 \
  --management.opentelemetry.tracing.export.otlp.endpoint=http://localhost:4318/v1/traces \
  --management.otlp.metrics.export.enabled=false > /tmp/app.log 2>&1 & APP=$!
for i in $(seq 60); do curl -fs -o /dev/null localhost:4000/actuator/health && break; sleep 0.5; done
for i in 1 2 3; do
  curl -s localhost:4000/graphql -H 'Content-Type: application/json' -H 'graphql-client-name: web' \
    -d '{"query":"query ClubPage { club(id: \"c1\") { name readingList { title reviews { rating } similar { title } } } }","operationName":"ClubPage"}' | head -c 200; echo
done
curl -s localhost:4000/graphql -H 'Content-Type: application/json' -d '{"query":"{ nope }"}'; echo
sleep 3
echo "== metric names"; curl -s localhost:4000/actuator/prometheus | grep -E '^graphql' | sed 's/ [0-9.E-]*$//' | head -40
echo "== tempo search"; traceql '{ }' 5 | jq -c '.[] | {rootServiceName, rootTraceName, durationMs}'
id=$(traceql '{ }' 5 | jq -r '.[0].traceID')
echo "== trace $id spans"; tempo_trace "$id" | jq -c '.trace.resourceSpans[].scopeSpans[].spans[] | {name, attrs: [.attributes[]? | "\(.key)=\(.value.stringValue // .value.intValue)"]}' | head -40
echo "== traceql slowest field"; traceql '{ name =~ "graphql field.*" } | avg(duration) > 1ms' 5 | jq -c '.[] | .rootTraceName'
