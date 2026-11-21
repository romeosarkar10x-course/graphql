#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file Phases.java || finish
cd "$LAB_DIR"
run() { timeout 60 java -cp "$(lib_cp)" Phases.java "$@" 2>/tmp/err.txt; }
check() { # name query vars
  run run "$LAB_HOME/tests/queries/$2.graphql" "$3" >/tmp/out.json
  if json_equal /tmp/out.json "$LAB_HOME/tests/expected/$1.json"; then pass "run $1: the spec response map"
  else fail "run $1 differs from the spec map (< yours, > expected)"; json_diff /tmp/out.json "$LAB_HOME/tests/expected/$1.json" | head -8
       [[ -s /tmp/err.txt ]] && tail -3 /tmp/err.txt | sed 's/^/        /'; fi
}
check club club '{"id":"c1"}'
check unknown-field unknown-field '{}'
check missing-club missing-club '{}'
check syntax syntax '{}'
check missing-variable club '{}'
run gen 5000 >/tmp/q5000.graphql
[[ $(grep -o 'hello' /tmp/q5000.graphql | wc -l) == 5000 ]] && pass "gen 5000 writes 5,000 fields" || fail "gen 5000 should print a query with 5,000 aliased hello fields"
run run /tmp/q5000.graphql '{}' >/tmp/out.json
grep -q "15,000" /tmp/out.json && pass "run keeps the default parser limits (the 5,000-field query is refused)" \
  || fail "run should use the default parser options: a 5,000-field query must hit the 15,000-token limit"
run phases /tmp/q5000.graphql >/tmp/ph.txt
ok=1; for p in parse validate execute; do grep -qE "^$p [0-9]+(\.[0-9]+)? ms$" /tmp/ph.txt || ok=0; done
(( ok )) && pass "phases prints parse, validate and execute times" || fail "phases should print 'parse <ms> ms', 'validate <ms> ms', 'execute <ms> ms'" "got: $(head -3 /tmp/ph.txt | tr '\n' '|')"
grep -qx "validation-errors 0" /tmp/ph.txt && grep -qx "fields 5000" /tmp/ph.txt \
  && pass "phases parses past the token limit and executes all 5,000 fields" \
  || fail "phases on the 5,000-field query should print 'validation-errors 0' and 'fields 5000'" "got: $(tail -2 /tmp/ph.txt | tr '\n' '|') $(tail -1 /tmp/err.txt)"
notes_check notes.md 60 m05/01
finish
