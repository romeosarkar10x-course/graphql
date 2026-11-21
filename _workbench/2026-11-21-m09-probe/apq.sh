cd /course/_workbench/2026-11-21-m09-probe/apq && mvn -q package -DskipTests 2>&1 | grep ERROR | head
java -jar target/shelfie-perf.jar > /tmp/app.log 2>&1 & pid=$!
for i in $(seq 60); do (exec 3<>/dev/tcp/localhost/4000) 2>/dev/null && break; sleep 0.5; done
python3 /course/tools/varnish.py 4000
Q='{ club(id: 1) { name owner { name } } }'
H=$(printf '%s' "$Q" | sha256sum | cut -d' ' -f1)
EXT="{\"persistedQuery\":{\"version\":1,\"sha256Hash\":\"$H\"}}"
echo "== 1 GET hash only, unknown";  curl -s -i -G localhost:4000/graphql --data-urlencode "extensions=$EXT" | grep -E "^HTTP|^\{"; echo
echo "== 2 GET query + hash (registers)"; curl -s -i -G localhost:4000/graphql --data-urlencode "query=$Q" --data-urlencode "extensions=$EXT" | grep -iE "^HTTP|^cache-control|^\{"; echo
echo "== 3 GET hash only, known"; curl -s -i -G localhost:4000/graphql --data-urlencode "extensions=$EXT" | grep -iE "^HTTP|^content-type|^\{"; echo
echo "== 4 known hash, other query text"; curl -s -G localhost:4000/graphql --data-urlencode "query={ club(id: 2) { name } }" --data-urlencode "extensions=$EXT"; echo
echo "== 4b new hash that does not match its text"; curl -s -G localhost:4000/graphql --data-urlencode "query={ club(id: 3) { name } }" --data-urlencode "extensions={\"persistedQuery\":{\"version\":1,\"sha256Hash\":\"0000000000000000000000000000000000000000000000000000000000000000\"}}"; echo
echo "== 5 GET mutation"; curl -s -i -G localhost:4000/graphql --data-urlencode 'query=mutation { renameBook(id: 1, title: "x") { title } }' | grep -E "^HTTP|^Allow|^\{"; echo
echo "== 6 via varnish x3"; for i in 1 2 3; do curl -s -o /dev/null -D - -G varnish:6081/graphql --data-urlencode "extensions=$EXT" | grep -iE "^x-cache|^age|^x-varnish|^cache-control|^HTTP" | tr -d '\r' | tr '\n' ' '; echo; done
echo "== 6b via varnish, POST x2"; for i in 1 2; do curl -s -o /dev/null -D - varnish:6081/graphql -H "Content-Type: application/json" -d "{\"query\":\"$Q\"}" | grep -iE "^x-cache|^HTTP" | tr -d "\r" | tr "\n" " "; echo; done
echo "== 6c via varnish, personal: X-Shelfie-User"; for u in 1 2; do curl -s -D - -G varnish:6081/graphql -H "X-Shelfie-User: $u" --data-urlencode "extensions=$EXT" | grep -iE "^x-cache" | tr -d "\r"; done
echo "== 7 bytes"; printf 'POST body full: '; jq -n --arg q "$(grep -v '^#' /course/modules/m09-performance/contract/dashboard.graphql)" '{query:$q}' | wc -c; printf 'GET url hash: '; printf '%s' "/graphql?extensions=$(jq -rn --arg e "$EXT" '$e|@uri')" | wc -c
kill $pid
