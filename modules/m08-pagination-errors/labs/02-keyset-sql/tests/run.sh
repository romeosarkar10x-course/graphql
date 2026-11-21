#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
require_file Keyset.java || finish
# 1,000,000 books in m08_02 (5 s to load); reloaded only when missing.
n=$(java -cp "$(lib_cp)" "$LAB_HOME/tests/Count.java" m08_02 2>/dev/null || echo 0)
[[ "$n" == 1000000 ]] || java -cp "$(lib_cp)" /course/modules/m08-pagination-errors/contract/Catalog.java m08_02 1000000 >/dev/null
rm -rf /tmp/keyset && mkdir -p /tmp/keyset
if javac -cp "$(lib_cp)" -d /tmp/keyset Keyset.java "$LAB_HOME/tests/KeysetCheck.java" 2>/tmp/keyset.log; then
  out=$(java -cp "/tmp/keyset:$(lib_cp)" KeysetCheck 2>&1); status=$?
  if (( status == 0 )); then pass "keyset paging over 1,000,000 rows ($(grep -c '^ok' <<<"$out") checks)"
  else fail "keyset paging: $(grep -c '^FAIL' <<<"$out") check(s) fail"; grep -vE '^(ok|info)' <<<"$out" | head -14 | sed 's/^/        /'; fi
  grep '^info' <<<"$out" | sed 's/^info  /        /'
else fail "Keyset.java does not compile with the harness" "$(grep error /tmp/keyset.log | head -3)"; fi
notes_check notes.md 60 m08/02
finish
