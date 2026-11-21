#!/usr/bin/env bash
# Start a lab app, run queries, print extensions.sql.  usage: naive.sh <lab dir> <schema>
cd /course/$1; java -cp "$(source /course/tools/testlib.sh; lib_cp)" /course/modules/m09-performance/contract/ClubData.java $2 >/dev/null
java -jar target/*.jar > /tmp/app.log 2>&1 & pid=$!
for i in $(seq 60); do (exec 3<>/dev/tcp/localhost/4000) 2>/dev/null && break; sleep 0.5; done
q() { jq -n --arg q "$1" '{query:$q}' | curl -s -H 'Content-Type: application/json' -d @- localhost:4000/graphql; }
D=$(grep -v '^#' /course/modules/m09-performance/contract/dashboard.graphql)
for i in 1 2 3; do s=$(date +%s%N); r=$(q "$D"); echo "dashboard: $(jq -c .extensions <<<"$r") $(( ($(date +%s%N)-s)/1000000 )) ms bytes $(wc -c <<<"$r")"; done
q "$D" | jq -S 'del(.extensions)' > /tmp/dashboard.json; md5sum /tmp/dashboard.json
echo "club 1: $(q '{ club(id: 1) { name owner { name } members(first: 3) { handle } } }' | jq -c .)"
echo "club 10 currentBook: $(q '{ club(id: 10) { currentBook { title } memberCount } }' | jq -c .)"
grep -c "Hibernate:" /tmp/app.log
kill $pid
