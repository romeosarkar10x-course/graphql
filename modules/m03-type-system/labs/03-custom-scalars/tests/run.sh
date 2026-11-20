#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file Isbn.java || finish
cd "$LAB_DIR"
run() { timeout 60 java -cp "$(lib_cp)" Isbn.java "$@" 2>/tmp/err.txt; }
for t in bad good; do
  run table "$LAB_HOME/tests/$t-inputs.json" >/tmp/$t.tsv
  if diff -q /tmp/$t.tsv "$LAB_HOME/tests/expected/$t.tsv" >/dev/null; then pass "$t inputs: literal and variable outcomes match"
  else fail "$t inputs differ (< yours, > expected)"; diff /tmp/$t.tsv "$LAB_HOME/tests/expected/$t.tsv" | head -10 | sed 's/^/        /'
       [[ -s /tmp/err.txt ]] && tail -2 /tmp/err.txt | sed 's/^/        /'; fi
done
for v in '"0-441-47812-3"' '"9780593135204"'; do
  run "$LAB_HOME/tests/book.graphql" "{\"isbn\": $v}" >/tmp/out.json
  name=$(tr -dc '0-9' <<<"$v")
  if json_equal /tmp/out.json "$LAB_HOME/tests/expected/book-$name.json"; then pass "book(isbn: $v) with the extended scalars"
  else fail "book(isbn: $v) differs"; json_diff /tmp/out.json "$LAB_HOME/tests/expected/book-$name.json" | head -8
       [[ -s /tmp/err.txt ]] && tail -2 /tmp/err.txt | sed 's/^/        /'; fi
done
run "$LAB_HOME/tests/books.graphql" >/tmp/out.json
json_equal /tmp/out.json "$LAB_HOME/tests/expected/books.json" && pass "books: isbn and listPrice serialise" || { fail "books differs"; json_diff /tmp/out.json "$LAB_HOME/tests/expected/books.json" | head -6; }
grep -q 'scalar ISBN @specifiedBy' schema.graphqls && pass "ISBN declares @specifiedBy" || fail "declare scalar ISBN with @specifiedBy(url: ...)"
notes_check notes.md 60 m03/03
finish
