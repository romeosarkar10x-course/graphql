#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
for f in OnError.java NormalizedCache.java; do require_file "$f" || finish; done
rm -rf /tmp/onerror && mkdir -p /tmp/onerror
if javac -cp "$(lib_cp)" -d /tmp/onerror OnError.java NormalizedCache.java "$LAB_HOME/solution/Page.java" "$LAB_HOME/tests/OnErrorCheck.java" 2>/tmp/onerror.log; then
  if out=$(java -cp "/tmp/onerror:$(lib_cp)" OnErrorCheck 2>&1); then pass "onError modes and the cache ($(grep -c '^ok' <<<"$out") checks)"
  else fail "onError: $(grep -c '^FAIL' <<<"$out") check(s) fail"; grep -vE '^ok' <<<"$out" | head -14 | sed 's/^FAIL  /        /'; fi
else fail "OnError.java and NormalizedCache.java do not compile with the harness" "$(grep error /tmp/onerror.log | head -3)"; fi
notes_check notes.md 60 m08/08
finish
