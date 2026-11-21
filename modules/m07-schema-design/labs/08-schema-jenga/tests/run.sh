#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR"
TOWER_KEY=4742efe95d425948c7dcc5fda1656feb6b68c84d54529f6d14d7047dce2570c9   # sha256 of the Tower column
DIFF_KEY=89b7e81fc85ad6da1b7db495faafa49c3806c2e46485f478c53409307050746a    # sha256 of the SchemaDiff column
require_file bets.md || finish
rows=$(grep -E '^\|\s*[0-9]{2}\s*\|' bets.md)
col() { awk -F'|' -v c="$1" '{gsub(/[ \t`*]/, "", $c); print toupper($c)}' <<<"$rows"; }
key() { col "$1" | paste -sd' ' | tr -d '\n' | sha256sum | cut -c1-64; }
if (( $(wc -l <<<"$rows") != 10 )); then fail "bets.md should keep its 10 rows"; finish; fi
bad=$(col 4 | grep -cvxE 'BREAKS|HOLDS'); (( bad == 0 )) && pass "a bet on all 10 cards" || fail "$bad bets missing (BREAKS or HOLDS)"
[[ $(key 5) == "$TOWER_KEY" ]] && pass "the Tower column is settled correctly (bin/course design clients)" || fail "the Tower column does not match the frozen clients' validation"
[[ $(key 7) == "$DIFF_KEY" ]] && pass "the SchemaDiff column matches graphql-java 26.1" || fail "the SchemaDiff column does not match bin/course design diff"
note "score: $(paste -d' ' <(col 4) <(col 5) | awk '$1==$2' | wc -l) of 10 bets right"
notes_check bets.md 60 m07/08
finish
