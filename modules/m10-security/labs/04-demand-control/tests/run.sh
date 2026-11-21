#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
T="java /course/modules/m10-security/contract/Token.java"
app_build target/shelfie-secure.jar || finish
app_start java -jar target/shelfie-secure.jar || finish
vars=(); for u in ada ben max olu root; do vars+=(--variable "$u=$($T $u)"); done
app_hurl "$LAB_HOME/tests/real.hurl" "real screens still work (real.hurl)" "${vars[@]}"
app_hurl "$LAB_HOME/tests/attacks.hurl" "attacks are refused (attacks.hurl)" --file-root "$LAB_HOME/tests"
# The budget: root spends a 2,110-point query five times in a row; 10,000 points a minute buys four.
q='{"query":"{ clubs(first: 10) { members(first: 10) { clubs(first: 10) { name } } } }"}'
tok=$($T root); out=()
for i in 1 2 3 4 5; do out+=("$(curl -s -H 'Content-Type: application/json' -H "Authorization: Bearer $tok" -d "$q" "$URL")"); done
ok4=$(for r in "${out[@]:0:4}"; do jq -r 'if .errors then "e" else "ok" end' <<<"$r"; done | grep -c ok)
fifth=$(jq -r '.errors[0].extensions.code // empty' <<<"${out[4]}"); retry=$(jq -r '.errors[0].extensions.retryAfterSeconds // 0' <<<"${out[4]}")
(( ok4 == 4 )) && [[ "$fifth" == RATE_LIMITED ]] && (( retry >= 1 )) \
  && pass "cost budget: four 2,110-point queries pass, the fifth is RATE_LIMITED with retryAfterSeconds" \
  || fail "cost budget: four 2,110-point queries pass, the fifth is RATE_LIMITED with retryAfterSeconds (got $ok4 ok, fifth: ${fifth:-no RATE_LIMITED})"
other=$(curl -s -H 'Content-Type: application/json' -H "Authorization: Bearer $($T ada)" -d "$q" "$URL" | jq -r 'if .errors then "limited" else "ok" end')
[[ "$other" == ok ]] && pass "budgets are per caller: ada is unaffected by root's spending" || fail "budgets are per caller: ada is unaffected by root's spending"
app_stop
notes_check notes.md 60 m10/04
finish
