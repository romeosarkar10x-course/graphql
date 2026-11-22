#!/usr/bin/env bash
source /course/tools/testlib.sh; source /course/modules/m15-federation/contract/fed.sh
cd /course/_workbench/2026-11-22-m15-probe/ops
compose sg-v1.yaml /tmp/sg.graphql || exit 1
sub_start accounts 4001 && sub_start catalog 4002 --spring.graphql.schema.locations=file:$PWD/catalog-v1/ --shelfie.catalog.review-book=true && sub_start reviews 4003 && sub_start clubs 4004 || exit 1
Q='{ latestReviews(first: 5) { id book { title } } }'
traffic() { local ok=0 bad=0 i r; for i in $(seq $2); do r=$(gql $1 "$Q"); if [[ $(jq -r '(.errors|length) == 0 and (.data.latestReviews[0].book.title != null)' <<<"$r" 2>/dev/null) == true ]]; then ok=$((ok+1)); else bad=$((bad+1)); echo "BAD: $r" | cut -c1-200 >> /tmp/bad.txt; fi; sleep 0.05; done; echo "traffic ok=$ok bad=$bad"; }
U=http://127.0.0.1:4000/graphql
ROUTER_CONFIG_FILE_PATH=hive-ops.yaml $FED_BIN/hive-router > /tmp/hive.log 2>&1 & FED_PIDS+=($!)
for i in $(seq 60); do curl -fs -o /dev/null 127.0.0.1:4000/health && break; sleep 0.5; done
echo "== hive plan shape"; gql $U "$Q" '{}' 'hive-expose-query-plan: true' | jq -c '.extensions|keys'; gql $U "$Q" '{}' 'hive-expose-query-plan: true' | jq -c '.extensions' | cut -c1-700
gql $U '{ me { handle } }' '{}' 'X-Shelfie-User: u3' | jq -c .
traffic $U 200 > /tmp/traffic.txt & T=$!
sleep 2; compose sg-v2.yaml /tmp/sg-new.graphql && mv /tmp/sg-new.graphql /tmp/sg.graphql; echo swapped
wait $T; cat /tmp/traffic.txt; head -3 /tmp/bad.txt 2>/dev/null
for p in 4002 4003; do sub_reset $p; done
gql $U '{ review(id: "r1") { book { title } } }' | jq -c .; echo "v2 catalog: $(sub_stats 4002 | jq -c .entityCalls)"
echo "-- percent(50) on hive"; for p in 4002; do sub_reset $p; done
compose sg-p50.yaml /tmp/sg-new.graphql && mv /tmp/sg-new.graphql /tmp/sg.graphql; sleep 3
for i in $(seq 40); do gql $U '{ review(id: "r1") { book { title } } }' > /tmp/p.json; done; jq -c . /tmp/p.json; echo "catalog: $(sub_stats 4002 | jq -c .entityCalls)"
kill ${FED_PIDS[-1]}; wait ${FED_PIDS[-1]} 2>/dev/null; unset 'FED_PIDS[-1]'
echo "-- custom label with expression"; sub_reset 4002
compose sg-label.yaml /tmp/sg.graphql
ROUTER_CONFIG_FILE_PATH=hive-ops2.yaml $FED_BIN/hive-router > /tmp/hive2.log 2>&1 & FED_PIDS+=($!)
for i in $(seq 60); do curl -fs -o /dev/null 127.0.0.1:4000/health && break; sleep 0.5; done
for i in $(seq 40); do gql $U '{ review(id: "r1") { book { title } } }' > /tmp/p.json; done; jq -c . /tmp/p.json; echo "catalog: $(sub_stats 4002 | jq -c .entityCalls)"
echo "-- defer"; curl -s $U -H 'content-type: application/json' -H 'accept: multipart/mixed' -d '{"query":"{ book(id: \"b1\") { title ... @defer { averageRating } } }"}' | head -c 500; echo
kill ${FED_PIDS[-1]}; wait ${FED_PIDS[-1]} 2>/dev/null; unset 'FED_PIDS[-1]'
echo "-- subscriptions enabled"; printf 'http:\n  host: 127.0.0.1\nsubscriptions:\n  enabled: true\n' > /tmp/hs.yaml
timeout 6 ROUTER_CONFIG_FILE_PATH=/tmp/hs.yaml $FED_BIN/hive-router 2>&1 | grep -i -m4 "subscri\|error\|listen" | cut -c1-300
ps aux | grep -c "[j]ava"
