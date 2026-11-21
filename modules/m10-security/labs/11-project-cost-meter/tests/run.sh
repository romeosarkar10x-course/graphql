#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
T="java /course/modules/m10-security/contract/Token.java"
# The API: lab 10.6's reference solution, built in /tmp.
API=/tmp/m10-11-api; rm -rf $API; cp -r /course/modules/m10-security/labs/06-public-api/solution $API
(cd $API && mvn -q package -DskipTests >/tmp/api-build.log 2>&1) || { fail "lab 10.6's API builds"; finish; }
java -jar $API/target/shelfie-secure.jar >/tmp/api.log 2>&1 & api=$!
java -cp "$(lib_cp)" CostMeter.java 4001 http://localhost:4000/graphql >/tmp/meter.log 2>&1 & meter=$!
trap 'kill $api $meter 2>/dev/null; wait $api $meter 2>/dev/null' EXIT
for _ in $(seq 120); do (exec 3<>/dev/tcp/localhost/4000) 2>/dev/null && (exec 3<>/dev/tcp/localhost/4001) 2>/dev/null && break; sleep 0.5; done
(exec 3<>/dev/tcp/localhost/4001) 2>/dev/null || { fail "CostMeter listens on 4001" "$(tail -3 /tmp/meter.log)"; finish; }
M=http://localhost:4001
send() { curl -s -H 'Content-Type: application/json' -H "X-Api-Key: $1" ${3:+-H "Authorization: Bearer $3"} -d "{\"query\":\"$2\"}" $M/graphql; }
direct=$(curl -s -H 'Content-Type: application/json' -H 'X-Api-Key: pk_reader_1' -d '{"query":"{ books(first: 3) { id title } }"}' http://localhost:4000/graphql | jq -c .data)
proxied=$(send pk_reader_1 '{ books(first: 3) { id title } }' | jq -c .data)
[[ -n "$direct" && "$direct" == "$proxied" ]] && pass "responses pass through unchanged" || fail "responses pass through unchanged"
for i in 1 2 3 4; do send pk_reader_1 '{ books(first: 10) { title } }' >/dev/null; done                         # 9 + 4 x 20
for i in 1 2 3; do send pk_writer_2 '{ books(first: 20) { id title reviews(first: 2) { rating } } }' >/dev/null; done   # 3 x 140
home='{ clubs(first: 10) { id name members(first: 10) { name } reviews(first: 5) { rating book { title } } } }'
for i in 1 2 3; do send pk_admin_4 "$home" >/dev/null; done                                                     # 430, 430, THROTTLED
send pk_writer_2 'mutation { addReview(clubId: \"c2\", bookId: \"b1\", rating: 5, body: \"metered\") { id } }' "$($T ada)" >/dev/null   # +2
m=$(curl -s $M/meter)
tot() { jq -r --arg k "$1" --arg f "$2" '[.clients[] | select(.key == $k) | .minutes[][$f]] | add // 0' <<<"$m"; }
[[ "$(tot pk_reader_1 cost) $(tot pk_reader_1 requests)" == "89 5" ]] && pass "bookworm-app: 89 points in 5 requests" \
  || fail "pk_reader_1 spent 89 points in 5 requests (got $(tot pk_reader_1 cost) in $(tot pk_reader_1 requests))"
[[ "$(tot pk_writer_2 cost) $(tot pk_writer_2 requests)" == "422 4" ]] && pass "review-bot: 422 points in 4 requests (a mutation included)" \
  || fail "pk_writer_2 spent 422 points in 4 requests (got $(tot pk_writer_2 cost) in $(tot pk_writer_2 requests))"
[[ "$(tot pk_admin_4 cost) $(tot pk_admin_4 requests) $(tot pk_admin_4 throttled)" == "860 3 1" ]] && pass "club-tool: 860 points, 3 requests, 1 throttled (spends nothing)" \
  || fail "pk_admin_4: 860 points, 3 requests, 1 throttled (got $(tot pk_admin_4 cost), $(tot pk_admin_4 requests), $(tot pk_admin_4 throttled))"
jq -e '[.clients[].minutes[].minute] | all(test("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}$"))' <<<"$m" >/dev/null \
  && pass "spend is bucketed by minute" || fail "minutes look like 2026-11-21T16:40"
page=$(curl -s -D /tmp/h $M/)
grep -qi 'content-type: text/html' /tmp/h && grep -q '/meter' <<<"$page" && pass "GET / is the dashboard, fed from /meter" \
  || fail "GET / serves an HTML dashboard that reads /meter"
finish
