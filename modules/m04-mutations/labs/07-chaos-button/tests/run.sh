#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR"
require_file Points.java || finish
rm -rf /tmp/chaos && mkdir -p /tmp/chaos
javac -cp "$(lib_cp)" -d /tmp/chaos Points.java "$LAB_HOME/tests/Chaos.java" 2>/tmp/chaos.log || { fail "Points.java does not compile with the chaos harness (keep Points.graphQL())" "$(grep error /tmp/chaos.log | head -2)"; finish; }
run() { timeout 120 java -cp "/tmp/chaos:$(lib_cp)" Chaos "$@" 2>&1; }
out=$(run 0 --calm); [[ $? == 0 ]] && pass "calm network: ${out#pass }" || fail "even without chaos the invariants break" "$(tail -n +2 <<<"$out" | head -3)"
for seed in 1 2 3 4 5; do
  out=$(run $seed)
  if [[ $? == 0 ]]; then pass "${out#pass }"
  else fail "$(head -1 <<<"$out" | sed 's/^fail //')" "$(tail -n +2 <<<"$out" | sed 's/^ *//' | head -3 | paste -sd ';' -)"; fi
done
notes_check notes.md 60 m04/07
finish
