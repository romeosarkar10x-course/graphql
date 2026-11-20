#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
GQL="java -cp $(lib_cp) /course/tools/gql/Gql.java"
GH_SCHEMA=/course/tools/schemas/github-2026-11-21.graphql

if mvn -q package >/tmp/gh-build.log 2>&1 && [[ -f target/gh-profile.jar ]]; then pass "mvn package builds target/gh-profile.jar"
else fail "mvn package failed"; tail -20 /tmp/gh-build.log | sed 's/^/        /'; finish; fi

# A fake api.github.com on localhost: logs every request, answers from tests/stub/<login>.json.
PORT=18$((RANDOM % 900 + 100)); LOG=/tmp/stub-requests.jsonl; : >"$LOG"
python3 "$LAB_HOME/tests/stub/stub.py" "$PORT" "$LOG" "$LAB_HOME/tests/stub" & STUB=$!
trap 'kill $STUB 2>/dev/null' EXIT
for _ in $(seq 50); do curl -s -o /dev/null "http://127.0.0.1:$PORT/" && break; sleep 0.1; done
cli() { GITHUB_TOKEN="${TOKEN-test-token}" timeout 30 java -jar target/gh-profile.jar "$@" --endpoint "http://127.0.0.1:$PORT/graphql"; }

# 1. No token
out=$(TOKEN= cli torvalds 2>&1); code=$?
[[ $code == 2 ]] && pass "no GITHUB_TOKEN: exit 2" || fail "no GITHUB_TOKEN: expected exit 2, got $code" "$out"
[[ ! -s "$LOG" ]] && pass "no GITHUB_TOKEN: no request sent" || fail "no GITHUB_TOKEN: the CLI should not call GitHub at all"

# 2. The card, from exactly one request
: >"$LOG"
cli torvalds >/tmp/card.txt 2>/tmp/err.txt; code=$?
if [[ $code == 0 ]] && diff -q /tmp/card.txt "$LAB_HOME/tests/expected/torvalds.txt" >/dev/null; then pass "torvalds: card matches"
else fail "torvalds: card differs (exit $code; < yours, > expected)"; diff /tmp/card.txt "$LAB_HOME/tests/expected/torvalds.txt" | head -12 | sed 's/^/        /'; tail -3 /tmp/err.txt | sed 's/^/        /'; fi
[[ $(grep -c . "$LOG") == 1 ]] && pass "torvalds: exactly one HTTP request" || fail "torvalds: expected 1 request, the stub saw $(grep -c . "$LOG")"
req=$(head -1 "$LOG")
[[ "$(jq -r '.headers.authorization' <<<"$req")" == "Bearer test-token" ]] && pass "sends Authorization: Bearer <token>" || fail "send the token as 'Authorization: Bearer <token>'"
[[ "$(jq -r '.body.variables.login' <<<"$req")" == "torvalds" ]] && pass "login travels in variables.login" || fail "send the login as variables.login, not inside the document"
jq -r '.body.query' <<<"$req" >/tmp/sent.graphql
grep -q 'torvalds' /tmp/sent.graphql && fail "the document contains the login: use \$login"
v=$($GQL validate /tmp/sent.graphql --schema "$GH_SCHEMA" 2>&1)
[[ "$v" == valid ]] && pass "the document is valid against GitHub's schema" || fail "the document is not valid against GitHub's schema" "$(head -3 <<<"$v")"
grep -qE '\$pinned *: *Int *= *6' /tmp/sent.graphql && pass "declares \$pinned: Int = 6" || fail "declare \$pinned: Int = 6 and use it for pinnedItems(first:)"

# 3. --pinned
: >"$LOG"; cli torvalds --pinned 2 >/dev/null 2>&1
[[ "$(head -1 "$LOG" | jq -r '.body.variables.pinned')" == 2 ]] && pass "--pinned 2 sends variables.pinned = 2" || fail "--pinned 2 should send variables.pinned = 2"

# 4. Sparse profile, missing user, GraphQL error
cli quiet-octo >/tmp/card.txt 2>/dev/null
diff -q /tmp/card.txt "$LAB_HOME/tests/expected/quiet-octo.txt" >/dev/null && pass "sparse profile (no name, empty location, no pins): card matches" \
  || { fail "sparse profile: card differs (< yours, > expected)"; diff /tmp/card.txt "$LAB_HOME/tests/expected/quiet-octo.txt" | head -8 | sed 's/^/        /'; }
out=$(cli nobody-here-x 2>&1); code=$?
[[ $code == 3 ]] && grep -q 'no GitHub user nobody-here-x' <<<"$out" && pass "unknown login: 'no GitHub user ...', exit 3" || fail "unknown login: expected 'no GitHub user nobody-here-x' and exit 3, got exit $code" "$out"
out=$(cli broken 2>&1); code=$?
[[ $code == 4 ]] && pass "GraphQL error without data: exit 4" || fail "GraphQL error without data: expected exit 4, got $code" "$out"

# 5. The real thing, when a token is available
if [[ -n "${GITHUB_TOKEN:-}" ]]; then
  out=$(timeout 30 java -jar target/gh-profile.jar torvalds 2>&1); code=$?
  [[ $code == 0 ]] && head -1 <<<"$out" | grep -q '(@torvalds)' && pass "live GitHub: card for torvalds" || fail "live GitHub run failed (exit $code)" "$(head -3 <<<"$out")"
else note "GITHUB_TOKEN not set: skipped the live GitHub run"; fi

lines=$(grep -cvE '^\s*(#.*)?$' notes.md 2>/dev/null || echo 0)
(( lines >= 8 )) && pass "notes.md filled in (rubric: bin/course grade m02/12)" || fail "notes.md: answer the questions"
finish
