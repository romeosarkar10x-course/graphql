cd /course/modules/m09-performance/labs/03-batch-loading/solution
java -jar target/shelfie-perf.jar > /tmp/app.log 2>&1 & pid=$!
for i in $(seq 60); do (exec 3<>/dev/tcp/localhost/4000) 2>/dev/null && break; sleep 0.5; done
echo "== GET with query"; curl -s -i -G localhost:4000/graphql --data-urlencode 'query={ club(id: 1) { name } }' | grep -iE "^HTTP|^cache|^content-type|data"
echo; echo "== GET mutation"; curl -s -i -G localhost:4000/graphql --data-urlencode 'query=mutation { renameBook(id: 1, title: "x") { title } }' | grep -iE "^HTTP|^allow|errors" ; echo
echo "== APQ hash only, POST"; curl -s -H 'Content-Type: application/json' -d '{"extensions":{"persistedQuery":{"version":1,"sha256Hash":"ecf4edb46db40b5132295c0291d62fb65d6759a9eedfa4d5d612dd5ec54a6b38"}}}' localhost:4000/graphql; echo
echo "== APQ hash only, GET"; curl -s -i -G localhost:4000/graphql --data-urlencode 'extensions={"persistedQuery":{"version":1,"sha256Hash":"ecf4edb46db40b5132295c0291d62fb65d6759a9eedfa4d5d612dd5ec54a6b38"}}' | grep -E "^HTTP|\{"
kill $pid
