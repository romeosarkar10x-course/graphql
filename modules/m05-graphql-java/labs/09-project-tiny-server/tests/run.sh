#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
if mvn -q package >/tmp/s-build.log 2>&1 && [[ -f target/shelfie-server.jar ]]; then pass "mvn package builds target/shelfie-server.jar"
else fail "mvn package failed"; tail -20 /tmp/s-build.log | sed 's/^/        /'; finish; fi
n=$(bash "$LAB_HOME/tests/count-lines.sh" src/main/java)
(( n <= 150 )) && pass "$n lines of server code (150 allowed; blank, comment, package and import lines not counted)" \
  || fail "$n lines of server code: the brief allows 150 (tests/count-lines.sh counts them)"
deps=$(grep -oE '<artifactId>[^<]+</artifactId>' pom.xml | sed 's/<[^>]*>//g' | grep -vE '^(shelfie-server|graphql-java|jackson-databind|maven-.*-plugin)$' | tr '\n' ' ')
[[ -z "$deps" ]] && pass "dependencies: graphql-java and Jackson only" || fail "no other dependencies allowed" "found: $deps"
PORT=4000; URL="http://localhost:$PORT/graphql"
listening() { (exec 3<>/dev/tcp/localhost/$PORT) 2>/dev/null; }
listening && { fail "port $PORT is already in use: stop whatever runs there first"; finish; }
java -jar target/shelfie-server.jar $PORT >/tmp/server.log 2>&1 & PID=$!
trap 'kill $PID 2>/dev/null; wait $PID 2>/dev/null' EXIT
for _ in $(seq 1 60); do listening && break; kill -0 $PID 2>/dev/null || break; sleep 0.5; done
listening || { fail "java -jar target/shelfie-server.jar $PORT did not listen" "$(tail -5 /tmp/server.log)"; finish; }
total=0; bad=0
for f in audit shelfie; do
  file="$LAB_HOME/tests/$f.hurl"; t=$(grep -cE '^# [AS][0-9]+' "$file"); total=$((total + t))
  if hurl --test --continue-on-error --variable "url=$URL" "$file" >/tmp/$f.log 2>&1; then pass "$f.hurl: all $t"
  else
    failed=$(grep -oE "$f.hurl:[0-9]+" /tmp/$f.log | cut -d: -f2 | sort -un | while read -r n; do
      awk -v n="$n" 'NR<=n && /^# [AS][0-9]/ {l=$0} END {print l}' "$file"; done | sort -u)
    k=$(echo "$failed" | grep -c .); bad=$((bad + k))
    fail "$f.hurl: $k of $t fail"; echo "$failed" | sed 's/^/        /'
  fi
done
note "$((total - bad)) of $total Hurl tests pass"
notes_check DESIGN.md 80 m05/09
finish
