#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR"
if mvn -q compile >/tmp/doctor-build.log 2>&1; then pass "mvn compile"
else fail "mvn compile failed"; tail -20 /tmp/doctor-build.log | sed 's/^/        /'; finish; fi

doctor() { env "$@" java -cp target/classes dev.shelfie.doctor.Doctor 2>&1; }

# 1. Healthy machine
out=$(doctor SHELFIE_URL="$REF" GITHUB_TOKEN=x); code=$?
[[ $code -eq 0 ]] && pass "healthy machine: exit 0" || fail "healthy machine: exit $code, expected 0" "$out"
for name in java hurl jq shelfie github-token; do
  grep -qE "^ok +$name\b" <<<"$out" && pass "healthy machine: 'ok $name' line" || fail "healthy machine: no 'ok $name' line"
done

# 2. hurl missing from PATH
out=$(doctor PATH=/opt/java/openjdk/bin:/usr/bin:/bin SHELFIE_URL="$REF" GITHUB_TOKEN=x); code=$?
[[ $code -ne 0 ]] && pass "hurl missing: non-zero exit" || fail "hurl missing: exit 0, expected failure"
grep -qE '^FAIL +hurl: .+' <<<"$out" && pass "hurl missing: 'FAIL hurl: <fix>' line" || fail "hurl missing: no 'FAIL hurl: ...' line" "$out"
grep -qE '^ok +jq\b' <<<"$out" && pass "hurl missing: jq still ok (checks are independent)" || fail "hurl missing: jq line wrong"

# 3. Server down
out=$(doctor SHELFIE_URL=http://localhost:1 GITHUB_TOKEN=x); code=$?
[[ $code -ne 0 ]] && pass "server down: non-zero exit" || fail "server down: exit 0, expected failure"
grep -qE '^FAIL +shelfie: .+' <<<"$out" && pass "server down: 'FAIL shelfie: <fix>' line" || fail "server down: no 'FAIL shelfie: ...' line" "$out"

# 4. No token is a warning, not a failure
out=$(env -u GITHUB_TOKEN SHELFIE_URL="$REF" java -cp target/classes dev.shelfie.doctor.Doctor 2>&1); code=$?
[[ $code -eq 0 ]] && pass "no token: still exit 0" || fail "no token: exit $code, a missing token must only warn"
grep -qE '^warn +github-token: .+' <<<"$out" && pass "no token: 'warn github-token: ...' line" || fail "no token: no warn line" "$out"
finish
