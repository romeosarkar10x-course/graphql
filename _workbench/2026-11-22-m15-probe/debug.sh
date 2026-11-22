cd /course/_workbench/2026-11-22-m15-probe
(cd spring && ./run.sh catalog-loader 4002 catalog > /tmp/catalog.log 2>&1 &)
(cd spring && ./run.sh reviews 4003 reviews > /tmp/reviews.log 2>&1 &)
for p in 4002 4003; do for i in $(seq 60); do curl -fs -o /dev/null localhost:$p/_stats && break; sleep 0.5; done; done
curl -s localhost:4003/graphql -H 'content-type: application/json' -d '{"query":"{ reviews { id rating book { __typename id } } }"}' | head -c 600; echo
curl -s localhost:4002/graphql -H 'content-type: application/json' -d '{"query":"query($r:[_Any!]!){ _entities(representations:$r){ ... on Book { id title } } }","variables":{"r":[{"__typename":"Book","id":"b1"}]}}' | head -c 300; echo
grep -B2 -A12 "Exception\|ERROR" /tmp/catalog.log | head -40; grep -A8 "Exception" /tmp/reviews.log | head -20
