#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR"
require_file club.graphqls || finish
if ! cmp -s club-page.graphql "$LAB_HOME/starter/club-page.graphql"; then fail "club-page.graphql was edited: restore it from the starter"; finish; fi
rm -rf /tmp/audit && mkdir -p /tmp/audit
if javac -cp "$(lib_cp)" -d /tmp/audit /course/tools/design/Design.java "$LAB_HOME/tests/AuditCheck.java" 2>/tmp/audit.log; then
  if out=$(java -cp "/tmp/audit:$(lib_cp)" AuditCheck 2>&1); then pass "nullability audit: 23 decisions and 4 outages ($(grep -c '^ok' <<<"$out") checks)"
  else fail "nullability audit"; grep -vE '^ok' <<<"$out" | head -14 | sed 's/^/        /'; fi
else fail "the harness does not compile" "$(grep error /tmp/audit.log | head -3)"; fi
notes_check audit.md 150 m07/04
finish
