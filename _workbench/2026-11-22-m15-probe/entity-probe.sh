#!/usr/bin/env bash
# Runs inside the course image: catalog (profile $1) + reviews subgraphs, Apollo Router, then counts _entities work.
cd /course/_workbench/2026-11-22-m15-probe
prof=${1:-catalog-single}
(cd spring && ./run.sh $prof 4002 catalog > /tmp/catalog.log 2>&1 &)
(cd spring && ./run.sh reviews 4003 reviews > /tmp/reviews.log 2>&1 &)
for p in 4002 4003; do for i in $(seq 60); do curl -fs -o /dev/null localhost:$p/_stats && break; sleep 0.5; done; done
/course/.cache/fed/bin/router --config compose/router.yaml --supergraph compose/supergraph.graphql > /tmp/router.log 2>&1 &
for i in $(seq 60); do curl -fs -o /dev/null localhost:8088/health && break; sleep 0.5; done
q='{"query":"{ reviews { id rating book { id title pages } } }"}'
curl -s localhost:4000 -H 'content-type: application/json' -H 'Apollo-Expose-Query-Plan: true' -d "$q" | jq -c '{data:.data, plan:.extensions.apolloQueryPlan.text}'
echo "$prof stats after one query: $(curl -s localhost:4002/_stats)"
curl -s localhost:4002/graphql -H 'content-type: application/json' -d '{"query":"{ _service { sdl } }"}' | jq -r .data._service.sdl | head -12
curl -s localhost:4002/graphql -H 'content-type: application/json' -d '{"query":"query($r:[_Any!]!){ _entities(representations:$r){ ... on Book { id title } } }","variables":{"r":[{"__typename":"Book","id":"b1"},{"__typename":"Book","id":"nope"}]}}' | jq -c .
grep -i -m3 "warn\|error\|licen" /tmp/router.log | cut -c1-300
