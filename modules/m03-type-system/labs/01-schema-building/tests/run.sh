#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file ShelfieSchema.java || finish
require_file schema.graphqls || finish
cd "$LAB_DIR"
run() { timeout 60 java -cp "$(lib_cp)" ShelfieSchema.java "$@" 2>/tmp/err.txt; }
check() { # name query vars
  run "$LAB_HOME/tests/queries/$2.graphql" "$3" >/tmp/out.json
  if json_equal /tmp/out.json "$LAB_HOME/tests/expected/$1.json"; then pass "$1 matches"
  else fail "$1 differs (< yours, > expected)"; json_diff /tmp/out.json "$LAB_HOME/tests/expected/$1.json" | head -12
       [[ -s /tmp/err.txt ]] && tail -3 /tmp/err.txt | sed 's/^/        /'; fi
}
check club-c1 club-page '{"id":"c1"}'
check club-c2 club-page '{"id":"c2"}'
check club-none club-page '{"id":"zz"}'
check clubs clubs '{}'
check book-b1 book '{"id":"b1"}'
check book-b5 book '{"id":"b5"}'
out=$(run --audit 2>&1); [[ -z "$out" ]] && pass "--audit: nothing unwired in schema.graphqls" || fail "--audit should print nothing for the lab schema" "got: $(echo $out | cut -c1-120)"
out=$(run --audit "$LAB_HOME/tests/extra.graphqls" 2>&1 | tr '\n' ' ')
[[ "$out" == "Book.subtitle Club.bannerUrl " ]] && pass "--audit finds the two unwired fields in an extension" \
  || fail "--audit with tests/extra.graphqls should print Book.subtitle and Club.bannerUrl, sorted, one per line" "got: $out"
notes_check notes.md 60 m03/01
finish
