#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file CallTree.java || finish
cd "$LAB_DIR"
run() { timeout 60 java -cp "$(lib_cp)" CallTree.java "$@" 2>/tmp/err.txt; }
run tree "$LAB_HOME/tests/queries/three-levels.graphql" >/tmp/tree.txt
if diff -q /tmp/tree.txt "$LAB_HOME/tests/expected/tree.txt" >/dev/null; then pass "tree: one line per fetcher call, 3 levels deep"
else fail "tree differs (< yours, > expected)"; diff /tmp/tree.txt "$LAB_HOME/tests/expected/tree.txt" | head -10 | sed 's/^/        /'
     [[ -s /tmp/err.txt ]] && tail -3 /tmp/err.txt | sed 's/^/        /'; fi
check() { # name label viewer
  run run "$LAB_HOME/tests/queries/$1.graphql" ${3:-} >/tmp/out.json
  if json_equal /tmp/out.json "$LAB_HOME/tests/expected/$1.json"; then pass "$2"
  else fail "$1 differs (< yours, > expected)"; json_diff /tmp/out.json "$LAB_HOME/tests/expected/$1.json" | head -10; fi
}
check viewer "viewer reads the id from GraphQLContext" u5
check roles "roleInClub reads the club from local context (null outside a club)"
check books-by-id "booksById: partial data plus one error per unknown id, with its path"
notes_check notes.md 60 m05/02
finish
