#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
if mvn -q package >/tmp/p-build.log 2>&1 && [[ -f target/shelfie-plumbing.jar ]]; then pass "mvn package builds target/shelfie-plumbing.jar"
else fail "mvn package failed"; tail -20 /tmp/p-build.log | sed 's/^/        /'; finish; fi
CP="target/classes:$(ls target/lib/*.jar | tr '\n' ':')"
rm -rf /tmp/harness && mkdir -p /tmp/harness
javac -cp "$CP" -d /tmp/harness "$LAB_HOME/tests/Harness.java" 2>/tmp/h.log || { fail "the test harness does not compile against your code (keep Plumbing's public methods)" "$(head -3 /tmp/h.log)"; finish; }
timeout 120 java -cp "/tmp/harness:$CP" Harness >/tmp/h.out 2>&1
while read -r verdict rest; do
  case "$verdict" in pass) pass "$rest" ;; fail) fail "${rest%% :: *}" "$([[ "$rest" == *" :: "* ]] && echo "${rest#* :: }")" ;; *) note "$verdict $rest" ;; esac
done </tmp/h.out
grep -rqE "maximumSize|maximumWeight|expireAfter" src/main/java && pass "the document cache is bounded" || fail "bound the document cache (maximumSize, or an expiry): an unbounded cache is a memory leak any client can trigger"
notes_check notes.md 60 m05/08
finish
