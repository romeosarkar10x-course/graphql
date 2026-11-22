#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
JAR=target/shelfie-privacy.jar
tok() { java /course/modules/m10-security/contract/Token.java "$@"; }
sql() { java -cp "$(lib_cp)" /course/modules/m11-realtime/contract/Sql.java "$1" 2>&1; }
OLU=$(tok oluwaseun); ADA=$(tok ada); ROOT=$(tok root)
gql() { curl -s "$URL" -H 'Content-Type: application/json' -H "Authorization: Bearer $1" -d "{\"query\":\"$2\"}"; }
app_build $JAR || finish
app_start java -jar $JAR || finish
app_hurl $LAB_HOME/tests/privacy.hurl "masking, soft delete and the audit log (privacy.hurl)" \
  --variable "olu=$OLU" --variable "ada=$ADA" --variable "root=$ROOT"

# Retention: a review deleted 40 days ago is purged, one deleted yesterday stays restorable.
old=$(gql "$ADA" 'mutation { createReview(bookId: \"b2\", rating: 2, body: \"Old news.\") { id } }' | jq -r .data.createReview.id)
new=$(gql "$ADA" 'mutation { createReview(bookId: \"b2\", rating: 3, body: \"Recent news.\") { id } }' | jq -r .data.createReview.id)
gql "$ADA" "mutation { a: deleteReview(id: \\\"$old\\\") b: deleteReview(id: \\\"$new\\\") }" >/dev/null
sql "UPDATE m14_06.reviews SET deleted_at = now() - interval '40 days' WHERE id = '$old'" >/dev/null
sql "UPDATE m14_06.reviews SET deleted_at = now() - interval '1 day' WHERE id = '$new'" >/dev/null
purged=$(gql "$ROOT" 'mutation { purgeDeletedReviews }' | jq -r .data.purgeDeletedReviews)
left=$(sql "SELECT string_agg(id, ',' ORDER BY id) FROM m14_06.reviews WHERE id IN ('$old', '$new')")
[[ $purged == 1 && $left == "$new" ]] && pass "retention: purgeDeletedReviews removes the 40-day-old delete, keeps yesterday's" \
  || fail "retention: purgeDeletedReviews removes the 40-day-old delete, keeps yesterday's" "returned $purged; rows left: ${left:-none}"
back=$(gql "$ADA" "mutation { restoreReview(id: \\\"$new\\\") { id } }" | jq -r .data.restoreReview.id)
[[ $back == "$new" ]] && pass "yesterday's delete can still be restored" || fail "yesterday's delete can still be restored" "restoreReview gave $back"

# Erasure: Oluwaseun asks to be forgotten.
PII='Adeyemi|oluwaseun|803 555|MARKER-9431|sister|Ikeja'
gql "$ADA" '{ user(id: \"u5\") { name } }' >/dev/null   # in every cache once more
out=$(gql "$OLU" 'mutation Forget { deleteMyAccount { erasedUserId reviewsDeleted } }')
echo "$out" | jq -e '.data.deleteMyAccount.erasedUserId == "u5" and .data.deleteMyAccount.reviewsDeleted == 3' >/dev/null \
  && pass "deleteMyAccount erases u5 and their 3 reviews" || fail "deleteMyAccount erases u5 and their 3 reviews" "${out:0:200}"
after=$(gql "$ADA" '{ user(id: \"u5\") { name email } book(id: \"b1\") { reviews { id body } } }')
if echo "$after" | jq -e '.data.user == null' >/dev/null && ! grep -qE "$PII" <<<"$after"; then pass "the API forgets: user(u5) is null, their reviews are gone (no cache kept them)"
else fail "the API forgets: user(u5) is null, their reviews are gone" "${after:0:200}"; fi
dump=$(for t in $(sql "SELECT table_name FROM information_schema.tables WHERE table_schema = 'm14_06'"); do sql "SELECT t::text FROM m14_06.$t t"; done)
hits=$(grep -iE "$PII" <<<"$dump")
[[ -z "$hits" ]] && pass "the database forgets: no table in m14_06 names them or quotes them" \
  || fail "the database forgets: no table in m14_06 names them or quotes them" "$(head -3 <<<"$hits" | cut -c1-120)"
actor=$(sql "SELECT count(*) FILTER (WHERE actor = 'u5'), count(*) FILTER (WHERE actor = 'erased') FROM m14_06.audit_log")
[[ $actor == $'0\t'* && ${actor#*$'\t'} -ge 2 ]] && pass "the audit log keeps the events, and loses the link: actor 'erased'" \
  || fail "the audit log keeps the events, and loses the link: actor 'erased'" "audit actors u5/erased: $actor"
logs=$(grep -iE "$PII|@example\.(net|com|org)" /tmp/app.log)
[[ -z "$logs" ]] && pass "the logs never held it: no name, email, phone or review text in the app's output" \
  || fail "the logs never held it: no name, email, phone or review text in the app's output" "$(head -2 <<<"$logs" | cut -c1-140)"
me=$(gql "$OLU" '{ me { id } }'); post=$(gql "$OLU" 'mutation { createReview(bookId: \"b1\", rating: 5, body: \"Back!\") { id } }')
[[ $(jq -r .data.me <<<"$me") == null && $(jq -r '.errors[0].extensions.classification' <<<"$post") == UNAUTHORIZED ]] \
  && pass "the old token belongs to nobody: me is null, writes are UNAUTHORIZED" || fail "the old token belongs to nobody" "${me:0:80} ${post:0:120}"
notes_check notes.md 80 m14/06
finish
