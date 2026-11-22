#!/usr/bin/env bash
# Java's reviews subgraph (Module 15 contract jar) against the TypeScript port: startup, memory, latency.
source /course/tools/testlib.sh; source /course/modules/m15-federation/contract/fed.sh
export PATH=$PATH:/course/.cache/node/bin
Q='{ latestReviews(first: 5) { id rating body author { handle } book { id } } }'
up() { for i in $(seq 600); do curl -fs -o /dev/null -H 'content-type: application/json' -d '{"query":"{ __typename }"}' localhost:$1/graphql && return 0; sleep 0.05; done; return 1; }
JAR=$(sub_jar)
t0=$(date +%s%N); java -XX:TieredStopAtLevel=1 -Xmx192m -jar $JAR --spring.profiles.active=reviews --server.port=4103 >/tmp/j.log 2>&1 & JP=$!
up 4103; t1=$(date +%s%N); echo "java startup_ms=$(( (t1 - t0) / 1000000 )) (flags as fed.sh: -XX:TieredStopAtLevel=1 -Xmx192m)"
java -cp "$(lib_cp)" $FED/Load.java http://localhost:4103/graphql 200 4 "$Q" >/dev/null   # warm-up
echo "java $(java -cp "$(lib_cp)" $FED/Load.java http://localhost:4103/graphql 1000 4 "$Q" $JP)"
kill $JP
cd /course/modules/m18-frontier/labs/06-port/solution && npm ci -s >/dev/null 2>&1 && npm run -s build
t0=$(date +%s%N); node dist/server.js 4104 >/tmp/n.log 2>&1 & NP=$!
up 4104; t1=$(date +%s%N); echo "node startup_ms=$(( (t1 - t0) / 1000000 )) (built; node $(node --version))"
java -cp "$(lib_cp)" $FED/Load.java http://localhost:4104/graphql 200 4 "$Q" >/dev/null
echo "node $(java -cp "$(lib_cp)" $FED/Load.java http://localhost:4104/graphql 1000 4 "$Q" $NP)"
kill $NP; rm -rf dist node_modules
