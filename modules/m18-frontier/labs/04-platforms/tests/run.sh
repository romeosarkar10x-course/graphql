#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
java -cp "$(lib_cp)" /course/modules/m09-performance/contract/ClubData.java m18_04 >/dev/null || { fail "could not load the club data into m18_04"; finish; }
[[ -f package-lock.json ]] || { fail "package-lock.json is missing (npm install once, then keep it)"; finish; }
npm ci --no-audit --no-fund >/tmp/pg-npm.log 2>&1 && pass "npm ci installs PostGraphile" || { fail "npm ci failed" "$(tail -3 /tmp/pg-npm.log)"; finish; }
PORT=5678; listening() { (exec 3<>/dev/tcp/localhost/$PORT) 2>/dev/null; }
listening && { fail "port $PORT is already in use"; finish; }
PORT=$PORT npx postgraphile -C graphile.config.mjs >/tmp/pg-server.log 2>&1 & PID=$!
trap 'kill $PID 2>/dev/null; wait $PID 2>/dev/null' EXIT
for _ in $(seq 120); do listening && break; kill -0 $PID 2>/dev/null || break; sleep 0.5; done
listening && pass "PostGraphile serves the generated API on :$PORT/graphql" || { fail "PostGraphile did not start" "$(tail -5 /tmp/pg-server.log)"; finish; }
t=$(grep -cE '^# G[0-9]+' $LAB_HOME/tests/generated.hurl)
if hurl --test --continue-on-error --variable url=http://localhost:$PORT/graphql $LAB_HOME/tests/generated.hurl >/tmp/pg-hurl.log 2>&1; then
  pass "the generated API, read-only and without the SQL helper: all $t checks (generated.hurl)"
else
  failed=$(grep -oE "generated.hurl:[0-9]+" /tmp/pg-hurl.log | cut -d: -f2 | sort -un | while read -r n; do
    awk -v n="$n" 'NR<=n && /^# G[0-9]/ {l=$0} END {print l}' $LAB_HOME/tests/generated.hurl; done | sort -u)
  fail "generated.hurl: $(echo "$failed" | grep -c .) of $t fail"; echo "$failed" | sed 's/^/        /'
fi
notes_check GAPS.md 150 m18/04
finish
