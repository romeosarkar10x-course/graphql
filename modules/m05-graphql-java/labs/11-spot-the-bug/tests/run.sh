#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file Bugs.java || finish
rm -rf /tmp/spot && mkdir -p /tmp/spot && cp "$LAB_DIR"/*.java "$LAB_DIR"/spot.graphqls /tmp/spot/ && cp "$LAB_HOME/tests/Harness.java" /tmp/spot/
cd /tmp/spot
timeout 120 java -cp "$(lib_cp)" Harness.java >/tmp/spot.out 2>/tmp/spot.err || [[ -s /tmp/spot.out ]] || { fail "the harness did not run" "$(grep -m3 error /tmp/spot.err)"; finish; }
fixed=0
while read -r verdict rest; do
  case "$verdict" in pass) pass "$rest"; fixed=$((fixed+1)) ;; fail) fail "${rest%% :: *}" "${rest#* :: }" ;; esac
done </tmp/spot.out
note "score: $fixed of 8 fixed"
notes_check bugs.md 80 m05/11
finish
