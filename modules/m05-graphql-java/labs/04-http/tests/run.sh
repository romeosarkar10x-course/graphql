#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file Server.java || finish
cd "$LAB_DIR"
PORT=4000; URL="http://localhost:$PORT/graphql"
listening() { (exec 3<>/dev/tcp/localhost/$PORT) 2>/dev/null; }
listening && { fail "port $PORT is already in use: stop whatever runs there first"; finish; }
java -cp "$(lib_cp)" Server.java $PORT >/tmp/server.log 2>&1 & PID=$!
trap 'kill $PID 2>/dev/null; wait $PID 2>/dev/null' EXIT
for _ in $(seq 1 60); do listening && break; kill -0 $PID 2>/dev/null || break; sleep 0.5; done
if ! listening; then fail "Server.java did not start on port $PORT" "$(tail -5 /tmp/server.log)"; finish; fi
pass "Server.java listens on $URL"
total=$(grep -c '^# A[0-9]' "$LAB_HOME/tests/audit.hurl")
# --continue-on-error: run every entry, so the report lists all failures, not just the first
if out=$(hurl --test --continue-on-error --variable "url=$URL" "$LAB_HOME/tests/audit.hurl" 2>&1); then pass "audit: all $total GraphQL-over-HTTP checks"
else
  failed=$(echo "$out" | grep -oE 'audit.hurl:[0-9]+' | cut -d: -f2 | while read -r n; do
    awk -v n="$n" 'NR<=n && /^# A[0-9]/ {l=$0} END {print l}' "$LAB_HOME/tests/audit.hurl"; done | sort -u)
  fail "audit: these checks fail ($(echo "$failed" | grep -c .) of $total)"; echo "$failed" | sed 's/^/        /'
fi
# Bonus: the same handler behind Javalin (javalin/). Reported, not required.
if [[ -f javalin/pom.xml ]] && ! grep -q 'TODO' javalin/JavalinServer.java 2>/dev/null; then
  kill $PID 2>/dev/null; wait $PID 2>/dev/null
  if (cd javalin && mvn -q package >/tmp/jv.log 2>&1); then
    (cd javalin && exec java -cp "target/classes:$(ls target/lib/*.jar | tr '\n' ':')" JavalinServer $PORT >/tmp/jv-server.log 2>&1) & PID=$!
    for _ in $(seq 1 60); do listening && break; sleep 0.5; done
    if hurl --test --variable "url=$URL" "$LAB_HOME/tests/audit.hurl" >/tmp/jv-audit.log 2>&1; then note "bonus: the Javalin server passes the same audit"
    else note "bonus: the Javalin server fails part of the audit (hurl --test ... yourself to see which)"; fi
  else note "bonus: javalin/ does not build: $(tail -1 /tmp/jv.log)"; fi
fi
notes_check notes.md 60 m05/04
finish
