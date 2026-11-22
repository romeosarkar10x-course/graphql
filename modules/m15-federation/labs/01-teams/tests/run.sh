#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
if [[ -f ownership.json ]] && jq -e . ownership.json >/dev/null 2>&1; then
  checks=0
  while IFS='|' read -r verdict name detail; do
    checks=$((checks+1))
    if [[ $verdict == ok ]]; then pass "$name"; else fail "$name" "$detail"; fi
  done < <(java -cp "$(lib_cp)" $LAB_HOME/tests/OwnershipCheck.java /course/modules/m15-federation/contract/monolith.graphqls ownership.json)
  (( checks > 0 )) || fail "the checker ran" "it printed nothing: see the error above"
else fail "ownership.json is valid JSON"; fi
notes_check notes.md 150 m15/01
finish
