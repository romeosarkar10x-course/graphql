#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
T="java /course/modules/m10-security/contract/Token.java"
app_build target/shelfie-secure.jar || finish
app_start java -jar target/shelfie-secure.jar || finish
app_hurl "$LAB_HOME/tests/public.hurl" "keys, scopes and budgets (public.hurl)" --variable "ada=$($T ada)" --variable "ben=$($T ben)" --variable "olu=$($T olu)"
# The experiment: 40 requests of cost 140 = 5,600 points through a 1,000-point bucket restoring 1,000 a second.
t0=$(date +%s%N)
if timeout 60 java -cp "$(lib_cp)" client/Spender.java "$URL" pk_spender_3 40 >/tmp/spender.log 2>&1; then ok=1; else ok=0; fi
secs=$(( ($(date +%s%N) - t0) / 1000000000 ))
u=$(curl -s "http://localhost:$PORT/_admin/usage" | jq -c '.spender // {}')
served=$(jq -r '.served // 0' <<<"$u"); throttled=$(jq -r '.throttled // 0' <<<"$u")
(( ok && served == 40 )) && pass "Spender got all 40 answers" || fail "Spender gets all 40 answers (served $served)" "$(tail -2 /tmp/spender.log)"
(( throttled <= 2 )) && pass "Spender backed off before the bucket ran dry ($throttled throttled)" \
  || fail "Spender backs off: at most 2 throttled responses (got $throttled)" "read extensions.cost and wait before a request it cannot pay for"
(( secs <= 15 )) && pass "Spender finished in ${secs}s (no needless waiting)" || fail "Spender finishes in 15 s or less (took ${secs}s)" "wait for what the next request costs, not a fixed second"
app_stop
notes_check notes.md 60 m10/06
finish
