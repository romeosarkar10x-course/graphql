#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m12-clients/contract/clients-test.sh
cd "$LAB_DIR" || exit 1
ref_up || { fail "the reference server is not up: run bin/course ref up"; finish; }
require_file src/main/resources/graphql-documents/clubDigest.graphql "the club digest document, as a file" || finish
app_build target/shelfie-digest.jar || finish
chat_start 4100 || { fail "the course chat server did not start"; finish; }
trap 'app_stop; chat_stop' EXIT
app_start java -jar target/shelfie-digest.jar || finish
B=http://localhost:$PORT; T=$(mktemp -d)
want='{"id":"c1","name":"The Midnight Readers","memberCount":8,"currentBook":{"title":"The Left Hand of Darkness","authors":["Ursula K. Le Guin"]},"owner":{"name":"Kemi Adeyemi","handle":"kemia"}}'
curl -s $B/clubs/c1 > $T/c1
[[ "$(jq -cS . $T/c1 2>/dev/null)" == "$(jq -cS . <<<"$want")" ]] && pass "GET /clubs/c1 (sync client, document file, toEntity)" \
  || fail "GET /clubs/c1 (sync client, document file, toEntity)" "got $(head -c 300 $T/c1)"
curl -s $B/clubs/c2/reactive > $T/c2
[[ "$(jq -r '.name + "|" + .owner.handle + "|" + (.memberCount|tostring)' $T/c2 2>/dev/null)" == "Slow Burn Mysteries|gracek|4" ]] \
  && pass "GET /clubs/c2/reactive (HttpGraphQlClient, Mono)" || fail "GET /clubs/c2/reactive (HttpGraphQlClient, Mono)" "got $(head -c 300 $T/c2)"
for p in /clubs/nope /clubs/nope/reactive; do
  s=$(curl -s -o /dev/null -w '%{http_code}' $B$p)
  [[ $s == 404 ]] && pass "GET $p is 404 (a null club)" || fail "GET $p is 404 (a null club)" "status $s"
done
post() { curl -s -o $T/r -w '%{http_code}' -H 'Content-Type: application/json' -H "X-User: $1" -d "{\"rating\":4,\"body\":\"$3\"}" $B/books/$2/reviews; }
s=$(post u11 b2 "Read it twice in one winter.")
author=$(curl -s -H 'Content-Type: application/json' -d '{"query":"{ user(id: \"u11\") { reviews { body book { id } } } }"}' $REF/graphql | jq -r '.data.user.reviews[] | select(.book.id=="b2") | .body')
[[ $s == 201 && $(jq -r .review.author.id $T/r) == u11 && "$author" == "Read it twice in one winter." ]] \
  && pass "POST /books/b2/reviews as u11: 201, and Shelfie stored it as u11's review (per-request header)" \
  || fail "POST /books/b2/reviews as u11: 201, and Shelfie stored it as u11's review (per-request header)" "status $s, body $(head -c 200 $T/r), Shelfie has: ${author:-nothing}"
s=$(post u11 b2 "Read it twice in one winter.")
[[ $s == 422 && $(jq -r '.userErrors[0].code' $T/r) == DUPLICATE ]] && pass "the same review again: 422 with the DUPLICATE userError" \
  || fail "the same review again: 422 with the DUPLICATE userError" "status $s, body $(head -c 200 $T/r)"
# Chat: the service subscribed to room r1 at startup.
sleep 1
ids=(); for b in one two three; do ids+=("$(chat_send r1 Ada "$b")"); done; chat_send r2 Ben "elsewhere" >/dev/null
sleep 1
have=$(curl -s $B/chat | jq -r '[.[] | .id + ":" + .author + ":" + .body] | join(",")')
[[ "$have" == "${ids[0]}:Ada:one,${ids[1]}:Ada:two,${ids[2]}:Ada:three" ]] && pass "GET /chat: r1's three messages in order, nothing from r2 (WebSocketGraphQlClient)" \
  || fail "GET /chat: r1's three messages in order, nothing from r2 (WebSocketGraphQlClient)" "got ${have:-nothing}"
chat_stop; sleep 2; chat_start 4100 || fail "the chat server did not restart"
ok=; for _ in $(seq 1 20); do id=$(chat_send r1 Cleo "after the restart"); sleep 0.5; curl -s $B/chat | jq -e '.[] | select(.body=="after the restart")' >/dev/null && { ok=1; break; }; done
[[ -n $ok ]] && pass "the subscription comes back after the chat server restarts" || fail "the subscription comes back after the chat server restarts" "no message sent after the restart arrived in 10 s"
curl -s $B/stats > $T/stats
[[ $(jq -r '.clubDigest.requests' $T/stats) == 4 && $(jq -r '.createReview.requests' $T/stats) == 2 ]] \
  && pass "GET /stats: an interceptor on both HTTP clients counted 4 clubDigest and 2 createReview" \
  || fail "GET /stats: an interceptor on both HTTP clients counted 4 clubDigest and 2 createReview" "got $(head -c 300 $T/stats)"
app_stop; chat_stop
ref_reset_data
notes_check notes.md 60 m12/02
finish
