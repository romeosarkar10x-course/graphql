#!/usr/bin/env bash
# Lab 16.2: usage reports in Hive's format (v2) to the course collector; 7 days of simulated traffic; Unused.java.
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m16-production/contract/ops.sh
source /course/modules/m16-production/contract/lab-app.sh
cd "$LAB_DIR" || exit 1
COL=; trap 'app_stop; kill $COL 2>/dev/null; ops_stop' EXIT
java -cp "$LIB/*" "$OPS/UsageCollector.java" >/tmp/collector.log 2>&1 & COL=$!
lab_app_up --management.tracing.enabled=false || finish
for _ in $(seq 1 40); do ops_up http://localhost:4200/_stats && break; sleep 0.5; done
reports() { curl -fsS localhost:4200/_reports; }
stats() { curl -fsS localhost:4200/_stats; }
wait_ops() { for _ in $(seq 1 ${2:-10}); do [[ $(stats | jq .operations) -ge $1 ]] && return 0; sleep 0.5; done; return 1; }

T0=$(( $(date +%s) * 1000 ))
gql_post '{"operationName":"Shelf","query":"query Shelf { books(first: 2) { title } }"}' "graphql-client-name: android" "graphql-client-version: 1.4.0" >/dev/null
if wait_ops 1 8; then pass "a report reaches the collector within 4 s"; else
  fail "a report reaches the collector within 4 s" "collector stats: $(stats); rejected reports are in its answer to your POST"; finish; fi
[[ $(stats | jq .rejected) == 0 ]] && pass "the collector accepted it (Hive v2 shape, headers)" || fail "the collector rejected a report" "$(stats)"
R=$(reports | jq -c '.[0] as $r | $r.operations[0] as $o | {o: $o, m: $r.map[$o.operationMapKey]}')
[[ $(jq -c '.m.fields | sort' <<<"$R") == '["Book.title","Query.books","Query.books.first"]' ]] \
  && pass "fields: field and argument coordinates" || fail "fields for { books(first: 2) { title } }" "got $(jq -c .m.fields <<<"$R")"
[[ $(jq -r '.m.operationName' <<<"$R") == Shelf && $(jq -r '.m.operation' <<<"$R") == *"books(first: 2)"* ]] \
  && pass "the map record keeps the document and operationName" || fail "map record: operation and operationName" "$(jq -c .m <<<"$R" | cut -c1-200)"
[[ $(jq -c '.o.metadata.client' <<<"$R") == '{"name":"android","version":"1.4.0"}' ]] && pass "client name and version from the headers" \
  || fail "metadata.client" "got $(jq -c .o.metadata <<<"$R")"
ts=$(jq '.o.timestamp' <<<"$R"); d=$(jq '.o.execution.duration' <<<"$R")
(( ts >= T0 - 2000 && ts <= T0 + 10000 )) && pass "timestamp in milliseconds, now" || fail "timestamp: ms since the epoch at the time of the request" "got $ts"
(( d > 10000 && d < 10000000000 )) && [[ $(jq '.o.execution.ok' <<<"$R") == true ]] && pass "duration in nanoseconds, ok true" || fail "execution: duration (ns) and ok" "$(jq -c .o.execution <<<"$R")"

curl -fsS -o /dev/null -X POST localhost:4200/_reset
gql_post '{"operationName":"Old","query":"query Old { book(id: \"b1\") { title } }"}' "graphql-client-name: ios" "graphql-client-version: 2.7.0" "X-Simulated-Time: 1793000000000" >/dev/null
gql_post '{"operationName":"Rate","query":"mutation Rate { addReview(bookId: \"b1\", rating: 9, body: \"x\") { id } }"}' "graphql-client-name: android" "graphql-client-version: 1.4.0" >/dev/null
gql_post '{"query":"{ nope }"}' >/dev/null
wait_ops 2 8; sleep 1.5
ops=$(reports | jq -c '[.[] | .map as $m | .operations[] | {t: .timestamp, x: .execution, n: $m[.operationMapKey].operationName}]')
[[ $(jq '[.[] | select(.n == "Old")][0].t' <<<"$ops") == 1793000000000 ]] && pass "X-Simulated-Time becomes the timestamp" \
  || fail "X-Simulated-Time: 1793000000000 must be the operation's timestamp" "$(jq -c '[.[] | select(.n == "Old")]' <<<"$ops")"
[[ $(jq -c '[.[] | select(.n == "Rate")][0].x | [.ok, .errorsTotal]' <<<"$ops") == '[false,1]' ]] && pass "errors: ok false, errorsTotal 1" \
  || fail "the failing mutation: execution.ok false, errorsTotal 1" "$(jq -c '[.[] | select(.n == "Rate")]' <<<"$ops")"
[[ $(stats | jq .rejected) == 0 && $(jq length <<<"$ops") == 2 ]] && pass "a document that does not validate is not reported (no fields to count)" \
  || fail "{ nope } must not be reported (Hive needs at least one field); got $(jq length <<<"$ops") operations, $(stats | jq .rejected) rejected reports"

curl -fsS -o /dev/null -X POST localhost:4200/_reset
java -cp "$LIB/*" "$OPS/Traffic.java" >/tmp/traffic.log 2>&1 || { fail "the 7-day replay ran" "$(tail -3 /tmp/traffic.log)"; finish; }
if wait_ops 1460 12; then pass "all 1,460 replayed operations reported"; else fail "all 1,460 replayed operations reported" "collector: $(stats)"; fi
n=$(stats | jq .reports); (( n >= 2 && n <= 300 )) && pass "batched: $n reports for 1,460 operations" || fail "batching: $n reports for 1,460 operations (one per operation is too many; one for all is too late)"

[[ -f Unused.java ]] || { fail "Unused.java exists"; finish; }
SCHEMA=src/main/resources/graphql/shelfie.graphqls
out=$(java -cp "$LIB/*" Unused.java $SCHEMA --now 2026-11-15T00:00:00Z --days 7 2>/tmp/unused.err)
if [[ "$(printf '%s\n' "$out" | sha256sum | cut -c1-16)" == "638c5cf7ed408de3" ]]; then pass "Unused.java: the fields no client used in the 7 days"
else fail "Unused.java: the fields no client used in the 7 days" "got $(grep -c . <<<"$out") lines: $(tr '\n' ' ' <<<"$out" | cut -c1-200) $(head -2 /tmp/unused.err)"; fi
out30=$(java -cp "$LIB/*" Unused.java $SCHEMA --now 2026-11-15T00:00:00Z --days 30 2>/dev/null)
if [[ "$(printf '%s\n' "$out30" | sha256sum | cut -c1-16)" == "77ebd1796097b8e1" ]]; then pass "Unused.java: the window is an option (30 days)"
else fail "Unused.java --days 30" "got $(grep -c . <<<"$out30") lines"; fi
finish
