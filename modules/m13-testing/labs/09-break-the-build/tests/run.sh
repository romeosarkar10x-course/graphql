#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
T=$LAB_HOME/tests
require_file calls.txt "one line per round: <round> safe|dangerous|breaking" || finish
right=0; total=0; wrong=()
while read -r round hashes; do
  [[ "$round" =~ ^[0-9]{2}$ ]] || continue
  total=$((total + 1))
  call=$(awk -v r="$round" '$1 == r {print tolower($2); exit}' calls.txt)
  h=$(printf '%s' "$round:$call" | sha256sum | cut -d' ' -f1)
  if [[ -n "$call" && " $hashes " == *" $h "* ]]; then right=$((right + 1)); else wrong+=("$round${call:+ ($call)}"); fi
done < $T/key.txt
(( right >= total - 2 )) && pass "$right of $total rounds called right (pass mark $((total - 2)))" \
  || fail "$right of $total rounds called right (pass mark $((total - 2)))" "look again at: ${wrong[*]}"
# What CI (graphql-java's SchemaDiff, as in lab 13.5) says, now that the calls are in.
verdicts=$(java -cp "$(lib_cp)" /course/modules/m13-testing/contract/RoundVerdicts.java $T/old.graphqls $T/rounds 2>/dev/null)
note "CI's verdicts (SchemaDiff): $(paste -sd' ' <<<"$verdicts")"
note "where your call and CI's differ, one of you is wrong: why.md says which, and why (rubric)"
notes_check why.md 120 m13/09
finish
