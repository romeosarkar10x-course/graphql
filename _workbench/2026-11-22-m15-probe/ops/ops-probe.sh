#!/usr/bin/env bash
source /course/tools/testlib.sh; source /course/modules/m15-federation/contract/fed.sh
cd /course/_workbench/2026-11-22-m15-probe/ops
compose sg-v1.yaml /tmp/sg.graphql || exit 1
sub_start accounts 4001 && sub_start catalog 4002 --spring.graphql.schema.locations=file:$PWD/catalog-v1/ --shelfie.catalog.review-book=true && sub_start reviews 4003 && sub_start clubs 4004 || exit 1
Q='{ latestReviews(first: 5) { id book { title } } }'
traffic() { # url n -> prints errors count
  local ok=0 bad=0 i r
  for i in $(seq $2); do r=$(gql $1 "$Q"); if [[ $(jq -r '(.errors|length) == 0 and (.data.latestReviews|length)==5 and (.data.latestReviews[0].book.title != null)' <<<"$r" 2>/dev/null) == true ]]; then ok=$((ok+1)); else bad=$((bad+1)); echo "BAD: $r" | cut -c1-200 >> /tmp/bad.txt; fi; sleep 0.05; done
  echo "traffic ok=$ok bad=$bad"
}
echo "== apollo"
$FED_BIN/router --config apollo-ops.yaml --supergraph /tmp/sg.graphql --listen 127.0.0.1:4000 --hot-reload > /tmp/apollo.log 2>&1 & FED_PIDS+=($!)
for i in $(seq 60); do curl -fs -o /dev/null 127.0.0.1:8088/health && break; sleep 0.5; done
U=http://127.0.0.1:4000/
gql $U "$Q" '{}' 'Apollo-Expose-Query-Plan: true' | jq -r .extensions.apolloQueryPlan.text | grep -o 'service: "[a-z]*"' | tr '\n' ' '; echo
gql $U '{ me { handle } }' '{}' 'X-Shelfie-User: u3' | jq -c .; sub_stats 4001 | jq -c .lastHeaders
traffic $U 200 > /tmp/traffic.txt & T=$!
sleep 2; compose sg-v2.yaml /tmp/sg-new.graphql && mv /tmp/sg-new.graphql /tmp/sg.graphql; echo "swapped to v2 at $(date +%T)"
wait $T; cat /tmp/traffic.txt; head -3 /tmp/bad.txt 2>/dev/null
gql $U "$Q" '{}' 'Apollo-Expose-Query-Plan: true' | jq -r .extensions.apolloQueryPlan.text | grep -o 'service: "[a-z]*"' | tr '\n' ' '; echo
grep -i -m3 "reload\|schema.*changed\|Updat" /tmp/apollo.log | cut -c1-250
echo "-- progressive override percent(50), no licence"
for p in 4002 4003; do sub_reset $p; done
compose sg-p50.yaml /tmp/sg-new.graphql && mv /tmp/sg-new.graphql /tmp/sg.graphql; sleep 4
for i in $(seq 40); do gql $U '{ review(id: "r1") { book { title } } }' > /tmp/p.json; done; jq -c . /tmp/p.json
echo "catalog: $(sub_stats 4002 | jq -c .entityCalls) reviews: $(sub_stats 4003 | jq -c .entityCalls)"
grep -i "licen\|override\|label\|enterprise" /tmp/apollo.log | tail -4 | cut -c1-300
echo "-- defer"
curl -s $U -H 'content-type: application/json' -H 'accept: multipart/mixed;deferSpec=20220824, application/json' -d '{"query":"{ book(id: \"b1\") { title ... @defer { averageRating } } }"}' | head -c 600; echo
kill ${FED_PIDS[-1]}; wait ${FED_PIDS[-1]} 2>/dev/null; unset 'FED_PIDS[-1]'
echo "-- apollo with subscriptions enabled"
printf 'subscription:\n  enabled: true\nhealth_check:\n  listen: 127.0.0.1:8088\n' > /tmp/sub.yaml
timeout 8 $FED_BIN/router --config /tmp/sub.yaml --supergraph /tmp/sg.graphql --listen 127.0.0.1:4000 2>&1 | grep -i "licen\|subscri\|error" | head -5 | cut -c1-400
