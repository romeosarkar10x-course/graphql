#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file Interfaces.java || finish
cd "$LAB_DIR"
check() { # name query vars
  timeout 60 java -cp "$(lib_cp)" Interfaces.java "$LAB_HOME/tests/$2.graphql" "${3:-{\}}" >/tmp/out.json 2>/tmp/err.txt
  if json_equal /tmp/out.json "$LAB_HOME/tests/expected/$1.json"; then pass "$1"
  else fail "$1 differs (< yours, > expected)"; json_diff /tmp/out.json "$LAB_HOME/tests/expected/$1.json" | head -8
       [[ -s /tmp/err.txt ]] && tail -2 /tmp/err.txt | sed 's/^/        /'; fi
}
for id in b1 c1 u1 r1 zz; do check node-$id node "{\"id\":\"$id\"}"; done
check nodes nodes
check reviewables reviewables
check recent recent
check introspect introspect
notes_check notes.md 60 m03/06
finish
