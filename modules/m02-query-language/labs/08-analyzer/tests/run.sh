#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file Analyzer.java || finish
run() { (cd "$LAB_DIR" && timeout 60 java -cp "$(lib_cp)" Analyzer.java "$@" 2>/tmp/err.txt); }

for f in "$LAB_HOME"/tests/fixtures/*.graphql; do
  b=$(basename "$f" .graphql)
  run "$f" >/tmp/out.txt; code=$?
  want_code=$(cat "$LAB_HOME/tests/expected/$b.exit")
  if [[ "$want_code" != 0 ]]; then
    [[ "$code" == "$want_code" && -s /tmp/err.txt ]] && pass "$b: rejected with a message ($(head -1 /tmp/err.txt | cut -c1-60))" \
      || fail "$b: should print an error to stderr and exit $want_code (got exit $code)"
  elif [[ "$code" == 0 ]] && diff -q <(jq -cS . /tmp/out.txt 2>/dev/null) <(jq -cS . "$LAB_HOME/tests/expected/$b.out") >/dev/null; then
    pass "$b: $(jq -c '{depth, fields, aliases}' /tmp/out.txt | tr '\n' ' ')"
  else fail "$b: metrics differ (< yours, > expected)"; diff <(jq -cS . /tmp/out.txt 2>&1) <(jq -cS . "$LAB_HOME/tests/expected/$b.out") | head -6 | sed 's/^/        /'
       [[ -s /tmp/err.txt ]] && tail -2 /tmp/err.txt | sed 's/^/        /'; fi
done

# --print must expand every spread, and the printed document must measure the same.
fx="$LAB_HOME/tests/fixtures/03-profile-fragments.graphql"
run "$fx" --print >/tmp/expanded.graphql
if grep -qE '\.\.\.[A-Za-z_]' /tmp/expanded.graphql || ! grep -q 'ProfilePage' /tmp/expanded.graphql; then
  fail "--print should print the operation with every named spread replaced by inline fragments"
else
  run /tmp/expanded.graphql >/tmp/again.txt
  diff -q <(jq -cS . /tmp/again.txt 2>/dev/null) <(jq -cS . "$LAB_HOME/tests/expected/03-profile-fragments.out") >/dev/null \
    && pass "--print output has no named spreads and measures the same" || fail "--print output measures differently from the original"
fi

lines=$(grep -cvE '^\s*(#.*)?$' "$LAB_DIR/notes.md" 2>/dev/null || echo 0)
(( lines >= 8 )) && pass "notes.md filled in (rubric: bin/course grade m02/08)" || fail "notes.md: answer the questions"
finish
