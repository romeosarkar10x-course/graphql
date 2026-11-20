#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file RestClubPage.java || finish
EXP=/course/modules/m01-why-graphql/contract/expected
for c in c1 c2 c3; do
  ref_reset_stats
  if ! (cd "$LAB_DIR" && timeout 60 java -cp "$(lib_cp)" RestClubPage.java "$c" >/tmp/out.json 2>/tmp/err.txt); then
    fail "$c: program failed"; tail -5 /tmp/err.txt | sed 's/^/        /'; continue
  fi
  if jq -e . /tmp/out.json >/dev/null 2>&1 && json_equal /tmp/out.json "$EXP/$c.json"; then pass "$c: page matches the contract"
  else fail "$c: page differs from contract/expected/$c.json (< yours, > expected)"; json_diff /tmp/out.json "$EXP/$c.json"; fi
  stats=$(ref_stats); claimed=$(tail -1 /tmp/err.txt | grep -oP 'requests=\K[0-9]+')
  real=$(jq .api <<<"$stats")
  [[ "$(jq .graphql <<<"$stats")" == 0 ]] || fail "$c: used /graphql; this lab is REST only"
  [[ -n "$claimed" && "$claimed" == "$real" ]] && pass "$c: requests=$claimed matches the server's count" \
    || fail "$c: stderr says requests=${claimed:-?} but the server counted $real" "last stderr line must be: requests=N bytes=B ms=T"
done
grep -qE '^\| c1 \| *[0-9]+' "$LAB_DIR/notes.md" 2>/dev/null && pass "notes.md has measurements (rubric: bin/course grade m01/02)" \
  || fail "notes.md: fill in the c1 row"
finish
