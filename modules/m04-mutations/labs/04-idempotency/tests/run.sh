#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
if mvn -q package >/tmp/w-build.log 2>&1 && [[ -f target/shelfie-writes.jar ]]; then pass "mvn package builds target/shelfie-writes.jar"
else fail "mvn package failed"; tail -20 /tmp/w-build.log | sed 's/^/        /'; finish; fi
CP="target/classes:$(ls target/lib/*.jar | tr '\n' ':')"
rm -rf /tmp/harness && mkdir -p /tmp/harness
javac -cp "$CP" -d /tmp/harness "$LAB_HOME/tests/Harness.java" 2>/tmp/h.log || { fail "the test harness does not compile against your code (keep Writes.graphQL(DataSource))" "$(head -3 /tmp/h.log)"; finish; }
timeout 120 java -cp "/tmp/harness:$CP" Harness >/tmp/h.out 2>&1
while read -r verdict rest; do
  case "$verdict" in pass) pass "$rest" ;; fail) fail "${rest%% :: *}" "$([[ "$rest" == *" :: "* ]] && echo "${rest#* :: }")" ;; *) note "$verdict $rest" ;; esac
done </tmp/h.out
notes_check notes.md 60 m04/04
finish
