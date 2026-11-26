#!/usr/bin/env bash
# The Go port, measured as Module 18's numbers.sh measured Java and TypeScript: startup, memory, latency.
source /course/tools/testlib.sh; source /course/modules/m15-federation/contract/fed.sh
Q='{ latestReviews(first: 5) { id rating body author { handle } book { id } } }'
up() { for i in $(seq 600); do curl -fs -o /dev/null -H 'content-type: application/json' -d '{"query":"{ __typename }"}' localhost:$1/graphql && return 0; sleep 0.05; done; return 1; }
cd /course/modules/m18-frontier/labs/06-port/solution-go
for run in 1 2 3; do
  t0=$(date +%s%N); ./reviews 4105 >/tmp/g.log 2>&1 & GP=$!
  up 4105; t1=$(date +%s%N); echo "go run $run startup_ms=$(( (t1 - t0) / 1000000 ))"
  java -cp "$(lib_cp)" $FED/Load.java http://localhost:4105/graphql 200 4 "$Q" >/dev/null
  echo "go run $run $(java -cp "$(lib_cp)" $FED/Load.java http://localhost:4105/graphql 1000 4 "$Q" $GP)"
  kill $GP; wait $GP 2>/dev/null
done
