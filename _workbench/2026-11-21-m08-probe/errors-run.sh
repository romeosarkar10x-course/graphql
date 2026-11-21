cd /course/modules/m08-pagination-errors/labs/06-error-strategy/${1:-solution} && mvn -q package -DskipTests 2>&1 | tail -5; java -jar target/shelfie-errors.jar > /tmp/e.log 2>&1 & for i in $(seq 80); do (exec 3<>/dev/tcp/localhost/4000) 2>/dev/null && break; sleep .5; done
r() { local accept="$1" q="$2"; shift 2; jq -n --arg q "$q" "{query:\$q}" | curl -s -w '  -> HTTP %{http_code} %{content_type}\n' -H "Content-Type: application/json" -H "Accept: $accept" "$@" -d @- localhost:4000/graphql; }
for a in application/json application/graphql-response+json; do
  echo "== Accept: $a"
  r $a '{ book(id: "b2") { title coverUrl } }'
  r $a '{ book(id: "b2") { founder } }'
  r $a '{ book(id: "b2") { title '
  r $a '{ myClubs { id } }'
  r $a 'mutation { joinClub(clubId: "c1") { id } }' -H 'X-Shelfie-User: u5'
  r $a 'mutation { joinClub(clubId: "c3") { id } }' -H 'X-Shelfie-User: u2'
  r $a 'mutation { joinClub(clubId: "c1") { id } }' -H 'X-Shelfie-User: u1'
  r $a 'mutation { deleteReview(id: "r1") }' -H 'X-Shelfie-User: u2' -H 'X-Request-Id: probe-42'
  r $a '{ hello }'
done
grep -E "errorId|ERROR" /tmp/e.log | head -4; kill %1
