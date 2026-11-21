#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file TwoWays.java || finish
cd "$LAB_DIR"
run() { timeout 90 java -cp "$(lib_cp)" TwoWays.java "$@" 2>/tmp/err.txt; }
same() { # label file expected
  if diff -q "$2" "$LAB_HOME/tests/expected/$3" >/dev/null; then pass "$1"
  else fail "$1 (< yours, > expected)"; diff "$2" "$LAB_HOME/tests/expected/$3" | head -8 | sed 's/^/        /'; tail -2 /tmp/err.txt | sed 's/^/        /'; fi
}
run sdl >/tmp/sdl.txt;  same "sdl: the schema from small.graphqls" /tmp/sdl.txt schema.graphqls
run code >/tmp/code.txt; same "code: the builder-made schema prints exactly the same SDL" /tmp/code.txt schema.graphqls
Q='{ club(id: "c2") { name memberCount owner { handle email } visibility } clubs { id } }'
a=$(run query sdl "$Q"); b=$(run query code "$Q")
[[ -n "$a" && "$a" == "$b" && "$a" == *gracek@shelfie.example* ]] && pass "query: both schemas answer the same, with the same fetchers" \
  || fail "query sdl and query code should give identical results" "sdl: ${a:0:100} | code: ${b:0:100}"
run public >/tmp/public.txt; same "public: SchemaTransformer drops the @internal field" /tmp/public.txt public.graphqls
run bench 2000 >/tmp/bench.txt
for k in sdl builder fast; do
  grep -qE "^$k 2000 types [0-9.]+ ms$" /tmp/bench.txt && pass "bench: $k builds 2,000 types" || fail "bench 2000 should print '$k 2000 types <ms> ms'" "got: $(grep "^$k" /tmp/bench.txt) $(tail -1 /tmp/err.txt)"
done
notes_check notes.md 60 m05/07
finish
