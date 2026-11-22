#!/usr/bin/env bash
# Probe 3: which spans and attributes Spring for GraphQL 2.1 + Boot 4.2's OpenTelemetry starter produce (defaults).
source /course/tools/testlib.sh; source /course/modules/m16-production/contract/ops.sh
trap 'kill $APP 2>/dev/null; ops_stop' EXIT
ops_start tempo || exit 1
reviews_start || exit 1
java -jar /course/modules/m16-production/contract/ops-app/target/shelfie-ops.jar --management.tracing.sampling.probability=1.0 \
  --management.opentelemetry.tracing.export.otlp.endpoint=http://localhost:4318/v1/traces --management.otlp.metrics.export.enabled=false --management.opentelemetry.tracing.export.schedule-delay=500ms > /tmp/app.log 2>&1 & APP=$!
for i in $(seq 60); do curl -fs -o /dev/null localhost:4000/actuator/health && break; sleep 0.5; done
TID=0af7651916cd43dd8448eb211c80319c
curl -s -o /dev/null localhost:4000/graphql -H "traceparent: 00-$TID-b7ad6b7169203331-01" -H 'Content-Type: application/json' \
  -d '{"query":"query ClubPage { club(id: \"c1\") { name readingList { title reviews { rating } similar { title } } } }","operationName":"ClubPage"}'
for i in $(seq 1 40); do tempo_trace $TID > /tmp/trace.json; [[ $(jq '.trace.resourceSpans|length' /tmp/trace.json) -gt 0 ]] && break; sleep 0.5; done; echo "trace found after ${i} polls of 0.5 s"
jq -r '.trace.resourceSpans[] | .resource.attributes | map("\(.key)=\(.value.stringValue)") | join(" ")' /tmp/trace.json | head -2
jq -r '.trace.resourceSpans[].scopeSpans[] | .scope.name as $s | .spans[] | "\((((.endTimeUnixNano|tonumber)-(.startTimeUnixNano|tonumber))/1000000*10|floor)/10) ms  \(.name)  [\($s)]  \([.attributes[]? | "\(.key)=\(.value.stringValue // .value.intValue // .value.boolValue)"] | join(" "))"' /tmp/trace.json | sort -k4 | head -40
echo "spans: $(jq '[.trace.resourceSpans[].scopeSpans[].spans[]] | length' /tmp/trace.json)"
