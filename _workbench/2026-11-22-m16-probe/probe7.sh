#!/usr/bin/env bash
# Probe 7: usage reports (Hive v2 shape) from 7 simulated days, and the cost extension.
source /course/tools/testlib.sh; source /course/modules/m16-production/contract/ops.sh
C=/course/modules/m16-production/contract
trap 'kill $APP $COL 2>/dev/null; ops_stop' EXIT
reviews_start || exit 1
java -cp "$LIB/*" $C/UsageCollector.java > /tmp/collector.log 2>&1 & COL=$!
java -jar $C/ops-app/target/shelfie-ops.jar --management.tracing.enabled=false > /tmp/app.log 2>&1 & APP=$!
for i in $(seq 60); do curl -fs -o /dev/null localhost:4000/actuator/health && curl -fs -o /dev/null localhost:4200/_stats && break; sleep 0.5; done
time java -cp "$LIB/*" $C/Traffic.java
sleep 2.5
curl -s localhost:4200/_stats; echo
curl -s localhost:4200/_reports > /tmp/reports.json
jq -c '.[0].operations[0]' /tmp/reports.json; jq -c '.[0].map | to_entries[0].value.fields' /tmp/reports.json
NOW=$(date -d 2026-11-15T00:00:00Z +%s)000; FROM=$(( NOW - 7*86400000 ))
jq -r --argjson from $FROM --argjson now $NOW '[.[] | .map as $m | .operations[] | select(.timestamp >= $from and .timestamp < $now) | $m[.operationMapKey].fields[]] | unique | .[]' /tmp/reports.json > /tmp/used.txt
wc -l < /tmp/used.txt; cat /tmp/used.txt | tr '\n' ' '; echo
echo "== cost"; for q in '{ books(first: 2) { title similar { title } } }' '{ club(id: \"c1\") { name members { name } readingList { title reviews { rating } } } }'; do
  curl -s localhost:4000/graphql -H 'Content-Type: application/json' -d "{\"query\":\"$q\"}" | jq -c '.extensions'; done
curl -s localhost:4000/actuator/prometheus | grep -E '^graphql_cost_(sum|count)' | head -4
grep -iE 'warn|error' /tmp/app.log | head -3
