#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file speedrun.jsh || finish
f="$LAB_DIR/speedrun.jsh"
out=$(cd "$LAB_DIR" && timeout 60 jshell -q speedrun.jsh </dev/null 2>/dev/null)
last=$(grep -vE '^\s*$' <<<"$out" | tail -1)
[[ "$last" == "A New Hope" ]] && pass "prints A New Hope" || { fail "last line is '${last:-<nothing>}', expected 'A New Hope'"; finish; }
grep -qE 'A New Hope' <(grep -vE '^\s*//' "$f") && fail "hard-coding the answer does not count"
code=$(grep -vE '^\s*(//.*)?$' "$f" | grep -vE '^\s*/exit\s*$')
lines=$(grep -c . <<<"$code"); chars=$(tr -d '\n' <<<"$code" | wc -c)
pass "score: $lines line(s), $chars characters (par: 2 lines)"
note "group play: add your score to games/leaderboards/jshell-speedrun.md"
finish
