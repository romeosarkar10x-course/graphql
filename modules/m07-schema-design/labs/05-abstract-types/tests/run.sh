#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR"
rm -rf /tmp/feed && mkdir -p /tmp/feed
if javac -cp "$(lib_cp)" -d /tmp/feed /course/tools/design/Design.java "$LAB_HOME/tests/FeedCheck.java" 2>/tmp/feed.log; then
  out=$(java -cp "/tmp/feed:$(lib_cp)" FeedCheck 2>&1)
  if [[ $? -eq 0 ]]; then pass "three feed designs, v1 and v2 ($(grep -c '^ok' <<<"$out") checks)"; grep '^info' <<<"$out" | sed 's/^info /        /'
  else fail "feed designs"; grep -vE '^(ok|info)' <<<"$out" | head -14 | sed 's/^/        /'; fi
else fail "the harness does not compile" "$(grep error /tmp/feed.log | head -3)"; fi
notes_check notes.md 120 m07/05
finish
