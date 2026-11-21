#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR"
SCREENS="$LAB_HOME/../../contract/screens"
design() { java -cp "$(lib_cp)" /course/tools/design/Design.java "$@"; }
require_file shelfie.graphqls || finish
if out=$(design lint shelfie.graphqls --rules naming,descriptions,references 2>&1); then pass "lint: naming, descriptions, references"
else fail "lint: naming, descriptions or references"; grep -E '^fail|rror' <<<"$out" | head -12 | sed 's/^/        /'; fi
if out=$(java -cp "$(lib_cp)" /course/tools/gql/Gql.java coordinates shelfie.graphqls 'Query.club(id:)' 'Query.book(id:)' 'Query.user(handle:)' 2>&1); then
  pass "entry points: club(id:), book(id:), user(handle:)"
else fail "entry points: the screens arrive with a club id, a book id and a user handle"; grep -E '^missing|rror' <<<"$out" | head -4 | sed 's/^/        /'; fi
for s in club book profile; do
  if out=$(design shape shelfie.graphqls "screens/$s-page.graphql" "$SCREENS/$s-page.json" 2>&1); then pass "$s page: the query's response has the screen's shape"
  else fail "$s page: response shape differs from $s-page.json"; head -10 <<<"$out" | sed 's/^/        /'; fi
done
notes_check notes.md 120 m07/01
finish
