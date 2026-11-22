#!/usr/bin/env bash
# Probe 4: the reference app's observability (custom conventions, client names, error counter, dependency spans).
source /course/tools/testlib.sh; source /course/modules/m16-production/contract/ops.sh
trap 'kill $APP 2>/dev/null; ops_stop' EXIT
ops_start tempo prometheus || exit 1
reviews_start || exit 1
java -jar /course/modules/m16-production/contract/ops-app/target/shelfie-ops.jar > /tmp/app.log 2>&1 & APP=$!
for i in $(seq 60); do curl -fs -o /dev/null localhost:4000/actuator/health && break; sleep 0.5; done
TID=1af7651916cd43dd8448eb211c80319c
q='{"query":"query ClubPage { club(id: \"c1\") { name readingList { title reviews { rating } similar { title } } } }","operationName":"ClubPage"}'
curl -s -o /dev/null localhost:4000/graphql -H "traceparent: 00-$TID-b7ad6b7169203331-01" -H 'graphql-client-name: web' -H 'Content-Type: application/json' -d "$q"
curl -s -o /dev/null localhost:4000/graphql -H 'graphql-client-name: evil-scraper-123' -H 'Content-Type: application/json' -d "$q"
curl -s localhost:4000/graphql -H 'graphql-client-name: ios' -H 'Content-Type: application/json' -d '{"query":"mutation Bad { addReview(bookId: \"b1\", rating: 9, body: \"x\") { id } }"}'; echo
curl -s -o /dev/null localhost:4000/graphql -H 'Content-Type: application/json' -d '{"query":"{ nope }"}'
for i in $(seq 1 40); do tempo_trace $TID > /tmp/trace.json; [[ $(jq '.trace.resourceSpans|length' /tmp/trace.json) -gt 0 ]] && break; sleep 0.5; done
jq -r '.trace.resourceSpans[].scopeSpans[] | .spans[] | "\((((.endTimeUnixNano|tonumber)-(.startTimeUnixNano|tonumber))/1000000*10|floor)/10) ms  \(.name)  \([.attributes[]? | select(.key|test("coordinate|operation.name|client|http.url|uri")) | "\(.key)=\(.value.stringValue // .value.intValue)"] | join(" "))"' /tmp/trace.json
curl -s localhost:4000/actuator/prometheus | grep -E '^graphql_(request_seconds_count|errors_total|datafetcher_seconds_count)' | sed 's/ [0-9.E-]*$//'
sleep 2; echo "== PromQL p95 by operation"; promq 'histogram_quantile(0.95, sum by (le, graphql_operation_name) (rate(graphql_request_seconds_bucket[1m])))'
curl -s localhost:4000/actuator/health/readiness; echo; curl -s localhost:4000/actuator/health/liveness; echo
