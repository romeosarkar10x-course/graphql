cd /course/modules/m09-performance/labs/07-measuring/solution/app
java -jar target/shelfie-perf.jar > /tmp/app.log 2>&1 & pid=$!
for i in $(seq 80); do (exec 3<>/dev/tcp/localhost/4000) 2>/dev/null && break; sleep 0.5; done
for i in $(seq 30); do curl -s -o /dev/null -H 'Content-Type: application/json' -d '{"query":"{ clubs { name } }"}' localhost:4000/graphql; done
curl -s localhost:4000/actuator/metrics | jq -c '[.names[] | select(startswith("graphql"))]'
curl -s 'localhost:4000/actuator/metrics/graphql.request.percentile' | jq -c .
curl -s 'localhost:4000/actuator/metrics/graphql.request.percentile?tag=phi:0.95' | jq -c '.measurements'
kill $pid
