#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR"
design() { java -cp "$(lib_cp)" /course/tools/design/Design.java "$@"; }
require_file good.graphqls || finish
RULES=naming,references,json-scalars,boolean-args,kitchen-sink,db-names,crud-names,timestamps
if out=$(design lint good.graphqls --rules $RULES 2>&1); then pass "good.graphqls passes all 8 anti-pattern rules"
elif grep -q '^fail' <<<"$out"; then fail "good.graphqls: $(grep -c '^fail' <<<"$out") anti-pattern findings"; grep -E '^fail' <<<"$out" | head -12 | sed 's/^/        /'
else fail "good.graphqls does not build into a schema" "$(grep -m1 -iE 'error|exception' <<<"$out" | cut -c1-160)"; fi
n=$(ls clients/*.graphql 2>/dev/null | wc -l)
if (( n == 7 )) && out=$(design clients good.graphqls clients/ 2>&1); then pass "all 7 clients rewritten and valid against good.graphqls"
else fail "the 7 clients in clients/ must be valid against good.graphqls ($n files)"; grep -E '^broken' <<<"$out" | cut -c1-200 | head -7 | sed 's/^/        /'; fi
if grep -qiE '\buser_?id\s*:' clients/*.graphql 2>/dev/null; then fail "a client still sends a user id: identity comes from the request (viewer), not from arguments"
else pass "no client passes its own user id"; fi
grep -qE '\b(rawQuery|deleteRow|sql)\b' good.graphqls && fail "good.graphqls still exposes raw SQL or table access" || pass "no raw SQL or table operations"
notes_check notes.md 200 m07/07
finish
