#!/usr/bin/env bash
# Probe 10: the OpenTelemetry Java agent 2.33.1 on lab 16.1's starter (no Micrometer tracing in the app).
source /course/tools/testlib.sh; source /course/modules/m16-production/contract/ops.sh
trap 'kill $APP 2>/dev/null; ops_stop' EXIT
cd /course/modules/m16-production/labs/01-observability/starter && mvn -q package -DskipTests || exit 1
AGENT=$(otel_agent) || exit 1; ls -la $AGENT
ops_start tempo || exit 1; reviews_start || exit 1
java -javaagent:$AGENT -Dotel.service.name=shelfie -Dotel.metrics.exporter=none -Dotel.logs.exporter=none \
  -Dotel.exporter.otlp.protocol=http/protobuf -Dotel.exporter.otlp.endpoint=http://localhost:4318 -Dotel.bsp.schedule.delay=500 $EXTRA \
  -jar target/shelfie-ops.jar > /tmp/app.log 2>&1 & APP=$!
for i in $(seq 90); do curl -fs -o /dev/null localhost:4000/actuator/health && break; sleep 0.5; done
TID=2af7651916cd43dd8448eb211c80319c
curl -s -o /dev/null localhost:4000/graphql -H "traceparent: 00-$TID-b7ad6b7169203331-01" -H 'Content-Type: application/json' \
  -d '{"query":"query ClubPage { club(id: \"c1\") { name readingList { title reviews { rating } similar { title } } } }","operationName":"ClubPage"}'
for i in $(seq 1 40); do tempo_trace $TID > /tmp/trace.json; [[ $(jq '.trace.resourceSpans|length' /tmp/trace.json) -gt 0 ]] && break; sleep 0.5; done
jq -r '.trace.resourceSpans[].scopeSpans[] | .scope.name as $s | .spans[] | "\((((.endTimeUnixNano|tonumber)-(.startTimeUnixNano|tonumber))/1000000*10|floor)/10) ms  \(.name)  [\($s)]  \([.attributes[]? | select(.key|test("graphql|url|http.route")) | "\(.key)=\(.value.stringValue // .value.intValue)"] | join(" "))"' /tmp/trace.json
echo "spans: $(jq '[.trace.resourceSpans[].scopeSpans[].spans[]] | length' /tmp/trace.json)"
