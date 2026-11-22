source /course/tools/testlib.sh
cd /course/modules/m15-federation/contract/plain
java -cp "$(lib_cp)" PlainService.java catalog 4101 > /tmp/cat.log 2>&1 &
java -cp "$(lib_cp)" PlainService.java reviews 4102 > /tmp/rev.log 2>&1 &
cd /course/_workbench/2026-11-22-m15-probe/nadel/app
java -jar target/shelfie-nadel.jar --schemas=../exp > /tmp/nadel.log 2>&1 &
for p in 4101 4102 4000; do for i in $(seq 60); do (exec 3<>/dev/tcp/localhost/$p) 2>/dev/null && break; sleep 0.5; done; done
sleep 1; head -5 /tmp/nadel.log
q() { jq -n --arg q "$1" '{query:$q}' | curl -s localhost:4000/graphql -H 'content-type: application/json' --data-binary @-; echo; }
q '{ latestReviews(first: 4) { id book { name } } }'
curl -s localhost:4101/_stats; echo
q '{ book(id: "b2") { title } }'
