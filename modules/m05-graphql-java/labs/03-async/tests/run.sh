#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file Siblings.java || finish
cd "$LAB_DIR"
ms() { timeout 20 java -cp "$(lib_cp)" Siblings.java "$@" >/tmp/sib.txt 2>/tmp/err.txt; head -1 /tmp/sib.txt | grep -oE '[0-9]+ ms' | grep -oE '[0-9]+'; }
between() { [[ -n "$1" ]] && (( $1 >= $2 && $1 <= $3 )); }
t=$(ms sync);       between "$t" 1990 4000 && pass "sync: 10 x 200 ms one after another ($t ms)" || fail "sync should take about 2,000 ms" "got: ${t:-no output} $(tail -1 /tmp/err.txt)"
t=$(ms platform);   between "$t" 590 1100  && pass "platform: 4 threads, 3 waves ($t ms)" || fail "platform (fixed pool of 4) should take about 600 ms: 3 waves of 200 ms" "got: ${t:-no output} ms"
t=$(ms virtual);    between "$t" 195 450   && pass "virtual: all 10 overlap ($t ms)" || fail "virtual should take about 200 ms: every sibling sleeps at the same time" "got: ${t:-no output} ms"
t=$(ms virtual 1000); between "$t" 195 1500 && pass "virtual: 1,000 sleeping siblings in $t ms" || fail "virtual with 1,000 siblings should still finish well under 1.5 s" "got: ${t:-no output} ms"
t=$(ms mutation 5)
if between "$t" 990 2000 && grep -qx "order s0 s1 s2 s3 s4" /tmp/sib.txt; then pass "mutation: async fetchers, still serial and in order ($t ms)"
else fail "mutation with 5 async fields should take about 1,000 ms and run s0..s4 in order" "got: ${t:-no output} ms, $(grep order /tmp/sib.txt)"; fi
notes_check notes.md 60 m05/03
finish
