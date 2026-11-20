#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file BookSearch.java || finish
require_file book-search.graphql "move the document into book-search.graphql" || finish

doc="$LAB_DIR/book-search.graphql"
grep -qE '\$text *: *String!' "$doc" && pass "declares \$text: String!" || fail "book-search.graphql: declare a required variable \$text: String!"
grep -qE '\$first *: *Int *= *5' "$doc" && pass "declares \$first: Int = 5" || fail "book-search.graphql: declare \$first: Int with default 5"
grep -q '"' "$doc" && fail "book-search.graphql contains a string literal: values belong in variables" || pass "no literal values in the document"

run() { # name, expected-file, args...
  local name=$1 exp=$2; shift 2
  (cd "$LAB_DIR" && timeout 60 java -cp "$(lib_cp)" BookSearch.java "$@" >/tmp/out.txt 2>/tmp/err.txt)
  if diff -q /tmp/out.txt "$LAB_HOME/tests/expected/$exp" >/dev/null; then pass "$name"
  else fail "$name: output differs (< yours, > expected)"; diff /tmp/out.txt "$LAB_HOME/tests/expected/$exp" | head -6 | sed 's/^/        /'
       [[ -s /tmp/err.txt ]] && tail -2 /tmp/err.txt | sed 's/^/        /'; fi
}
run "'the' uses the default limit of 5" the.txt the
run "'the' 2 sends a limit" the-2.txt the 2
run "'NAME' ignores case" name.txt NAME
run "no match" none.txt zzz
run "input with quotes and a backslash is just text" none.txt 'say "hi" \ x'
run "the injection payload from lecture 2.2 is just text" none.txt '", first: 20) { publishedYear title } __schema { types { name } } } #'

lines=$(grep -cvE '^\s*(#.*)?$' "$LAB_DIR/notes.md" 2>/dev/null || echo 0)
(( lines >= 10 )) && pass "notes.md filled in (rubric: bin/course grade m02/02)" || fail "notes.md: write up the injection experiment"
finish
