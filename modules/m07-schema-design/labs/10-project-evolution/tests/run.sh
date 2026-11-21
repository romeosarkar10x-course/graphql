#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR"
[[ -f Sim.java ]] || { fail "Sim.java is missing: copy it back from the starter"; finish; }
if ! cmp -s Sim.java "$LAB_HOME/starter/Sim.java"; then fail "Sim.java was edited: copy it back from the starter"; finish; fi
out=$(java -cp "$(lib_cp)" Sim.java check 2>&1)
shipped=$(grep -c ' shipped: ' <<<"$out")
if grep -q '^all 6 change requests shipped' <<<"$out"; then pass "all 6 change requests shipped with zero client breakage ($(grep -c '  ok ' <<<"$out") checks)"
else fail "change requests: $shipped of 6 shipped"; grep -E '^\s+fail|^missing|problem' <<<"$out" | head -8 | sed 's/^/        /'; fi
if [[ -f steps/06.graphqls ]]; then
  if out=$(java -cp "$(lib_cp)" /course/tools/design/Design.java lint steps/06.graphqls --rules naming,references 2>&1); then pass "the final schema passes naming and references"
  else fail "the final schema: naming or references"; grep '^fail' <<<"$out" | head -5 | sed 's/^/        /'; fi
fi
notes_check adr.md 250 m07/10
finish
