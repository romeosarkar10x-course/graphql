#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
AUDIT_SRC=/course/modules/m17-under-the-hood/contract/http-audit AUDIT=/course/.cache/m17-http-audit
# The four warnings where graphql-http 1.23.1 asks for what the September 2026 edition forbids (see the README).
EDITION_WARNINGS="572B FDE2 7B9B 86EE"
if mvn -q package >/tmp/h-build.log 2>&1 && [[ -f target/shelfie-server.jar ]]; then pass "mvn package builds target/shelfie-server.jar"
else fail "mvn package failed"; tail -20 /tmp/h-build.log | sed 's/^/        /'; finish; fi
if ! cmp -s "$AUDIT_SRC/package-lock.json" "$AUDIT/package-lock.json" 2>/dev/null || [[ ! -d $AUDIT/node_modules ]]; then
  rm -rf "$AUDIT"; mkdir -p "$AUDIT" && cp "$AUDIT_SRC"/{package.json,package-lock.json,audit.mjs} "$AUDIT"/
  (cd "$AUDIT" && npm ci --silent --ignore-scripts) >/tmp/h-npm.log 2>&1 || { fail "npm could not install graphql-http 1.23.1" "$(tail -3 /tmp/h-npm.log)"; finish; }
fi
PORT=4000; URL="http://localhost:$PORT/graphql"
listening() { (exec 3<>/dev/tcp/localhost/$PORT) 2>/dev/null; }
listening && { fail "port $PORT is already in use: stop whatever runs there first"; finish; }
java -jar target/shelfie-server.jar $PORT >/tmp/h-server.log 2>&1 & PID=$!
trap 'kill $PID 2>/dev/null; wait $PID 2>/dev/null' EXIT
for _ in $(seq 1 60); do listening && break; kill -0 $PID 2>/dev/null || break; sleep 0.5; done
listening || { fail "java -jar target/shelfie-server.jar $PORT did not listen" "$(tail -5 /tmp/h-server.log)"; finish; }

node "$AUDIT/audit.mjs" "$URL" >/tmp/h-audit.txt 2>&1
summary=$(tail -1 /tmp/h-audit.txt)
errors=$(grep -c '^error' /tmp/h-audit.txt); total=$(jq -r '.total // 0' <<<"$summary" 2>/dev/null)
(( total > 0 )) || { fail "graphql-http's audit did not run" "$(tail -3 /tmp/h-audit.txt)"; finish; }
(( errors == 0 )) && pass "graphql-http audit: every MUST holds ($summary)" \
  || { fail "graphql-http audit: $errors MUST rule(s) broken"; grep '^error' /tmp/h-audit.txt | cut -c1-200 | sed 's/^/        /'; }
other=$(grep '^warn' /tmp/h-audit.txt | awk -v ok="$EDITION_WARNINGS" 'BEGIN{n=split(ok,a," "); for(i=1;i<=n;i++) allowed[a[i]]=1} !($2 in allowed)')
[[ -z "$other" ]] && pass "graphql-http audit: no SHOULD warnings except the four the September 2026 edition overrules" \
  || { fail "graphql-http audit: SHOULD warnings to fix"; cut -c1-200 <<<"$other" | sed 's/^/        /'; }
for id in $EDITION_WARNINGS; do
  grep -qE "^(warn|ok) +$id " /tmp/h-audit.txt || fail "graphql-http audit: check $id is missing from the report"
done
notices=$(grep '^notice' /tmp/h-audit.txt | grep -v 'MAY use 400 status code')
[[ -z "$notices" ]] && pass "graphql-http audit: the only notices are its MAY-400s, where the edition says 422" \
  || { fail "graphql-http audit: notices to look at"; cut -c1-200 <<<"$notices" | sed 's/^/        /'; }

HURL=/course/modules/m05-graphql-java/labs/09-project-tiny-server/tests/audit.hurl; t=$(grep -cE '^# [AS][0-9]+' "$HURL")
if hurl --test --continue-on-error --variable "url=$URL" "$HURL" >/tmp/h-hurl.log 2>&1; then pass "Module 5's audit of the September 2026 edition still passes: all $t checks"
else fail "Module 5's audit of the September 2026 edition: some of the $t checks fail"; grep -oE "audit.hurl:[0-9]+" /tmp/h-hurl.log | sort -u | head -5 | sed 's/^/        /'; fi

code=$(curl -s -o /tmp/h-batch.json -w '%{http_code}' "$URL" -H 'Content-Type: application/json' -H 'Accept: application/graphql-response+json' \
  -d '[{"query":"{ hello }"},{"query":"{ clubs { id } }"}]')
[[ "$code" =~ ^4 ]] && ! grep -q '"data"' /tmp/h-batch.json && pass "a batch (JSON array) is refused with $code and not executed" \
  || fail "a batch (JSON array) is refused with a 4xx and not executed" "got $code: $(head -c 200 /tmp/h-batch.json)"
require_file AUDIT.md "your audit notes" && notes_check AUDIT.md 120 m17/08
finish
