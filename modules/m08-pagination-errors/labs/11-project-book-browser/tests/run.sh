#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
if mvn -q package -DskipTests >/tmp/tui-build.log 2>&1 && [[ -f target/book-browser.jar ]]; then pass "mvn package builds target/book-browser.jar"
else fail "mvn package failed"; grep -E "ERROR|error:" /tmp/tui-build.log | head -8 | sed 's/^/        /'; finish; fi
CP="target/book-browser.jar:$(ls target/lib/*.jar | tr '\n' ':')"
rm -rf /tmp/tui && mkdir -p /tmp/tui
javac -cp "$CP" -d /tmp/tui "$LAB_HOME/tests/TuiCheck.java" 2>/tmp/tui.log || { fail "the harness does not compile against your App" "$(grep error /tmp/tui.log | head -3)"; finish; }
listening() { (exec 3<>/dev/tcp/localhost/4000) 2>/dev/null; }
round() { # $1 mode, rest: FeedServer options
  local mode="$1"; shift
  java -cp "$(lib_cp)" /course/modules/m08-pagination-errors/contract/FeedServer.java "$@" >/tmp/feed.log 2>&1 & local pid=$!
  for _ in $(seq 60); do listening && break; sleep 0.5; done
  out=$(timeout 60 java -cp "/tmp/tui:$CP" TuiCheck http://localhost:4000/graphql "$mode" 2>&1)
  kill $pid; wait $pid 2>/dev/null
  if grep -q '^FAIL' <<<"$out" || ! grep -q '^ok' <<<"$out"; then
    fail "$mode feed: $(grep -c '^FAIL' <<<"$out") check(s) fail"; grep -vE '^ok' <<<"$out" | head -10 | sed 's/^FAIL  /        /; s/^/        /'
  else pass "$mode feed: $(grep -c '^ok' <<<"$out") screen checks"; fi
}
round fixed --books 200 --every 0
round live --books 300 --every 30
finish
