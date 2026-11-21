#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
# 1. Four or more configurations, covering both collectors and both thread models.
cfg=$(grep -vE '^\s*(#|$)' configs.txt 2>/dev/null)
names=$(cut -d: -f1 <<<"$cfg" | tr -d ' ' | sort -u | grep -c .)
if (( names >= 4 )) && grep -q -- '-XX:+UseZGC' <<<"$cfg" && grep -q 'spring.threads.virtual.enabled=true' <<<"$cfg" \
   && grep -vq -- '-XX:+UseZGC' <<<"$cfg"
then pass "configs.txt: $names configurations, G1 and ZGC, platform and virtual threads"
else fail "configs.txt: at least 4 configurations, G1 and ZGC, platform and virtual threads" "$names configurations"; fi
# 2. You ran them all: a results row per configuration.
missing=0
while read -r n; do grep -qE "^\| $n \| .*\| [0-9][0-9,]* \| [0-9][0-9,]* \| [0-9][0-9,]* \|" results/tuning.md 2>/dev/null || missing=$((missing + 1)); done < <(cut -d: -f1 <<<"$cfg" | tr -d ' ')
(( missing == 0 && names > 0 )) && pass "results/tuning.md has a measured row for every configuration" || fail "results/tuning.md has a measured row for every configuration" "$missing missing: run ./tune.sh"
# 3. The document cache (lecture 9.8).
grep -rq 'PreparsedDocumentProvider' app/src/main/java 2>/dev/null && pass "the app caches parsed documents" || fail "the app caches parsed documents" "a PreparsedDocumentProvider (lab 9.7 measured what it saves)"
# 4. The winner, re-run by the harness at a lighter load: it must start, hold, and use the collector it claims.
winner=$(grep -m1 -oE '^Winner: *[A-Za-z0-9_.-]+' tuning-report.md 2>/dev/null | sed 's/Winner: *//')
line=$(grep -E "^ *$winner *:" <<<"$cfg" | head -1)
if [[ -n "$winner" && -n "$line" ]]; then
  t=/tmp/m08-tune; rm -rf $t; mkdir -p $t; echo "$line" > $t/configs.txt
  cp -r app load measure.sh tune.sh $t/
  (cd $t && RATE=50 DURATION=10 HEAP="${HEAP:-1g}" timeout 400 ./tune.sh >/tmp/tune-check.log 2>&1)
  row=$(grep -E "^\| $winner \|" $t/results/tuning.md 2>/dev/null)
  ko=$(awk -F'|' '{gsub(/ /, "", $7); print $7}' <<<"$row")
  gc=$(awk -F'|' '{print $9}' <<<"$row")
  want=G1; grep -q -- '-XX:+UseZGC' <<<"$line" && want="Z Garbage"
  [[ "$ko" == 0 ]] && grep -q "$want" <<<"$gc" && pass "the winner ($winner) holds 50 dashboards/s with no failures, on$gc" \
    || fail "the winner ($winner) holds 50 dashboards/s with no failures" "$(tail -3 /tmp/tune-check.log) $row"
  rm -rf $t
else fail "tuning-report.md names a winner from configs.txt" "a line 'Winner: <config name>'"; fi
notes_check tuning-report.md 150 m09/08
finish
