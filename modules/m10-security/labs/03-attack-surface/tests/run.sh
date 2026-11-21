#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
C=/course/modules/m10-security/contract
URL=http://localhost:4000/graphql; export URL
$C/vulnerable.sh stop >/dev/null 2>&1 || true
$C/vulnerable.sh start >/tmp/vuln-start.log 2>&1 || { fail "the vulnerable fork starts" "$(tail -3 /tmp/vuln-start.log)"; finish; }
trap '$C/vulnerable.sh stop' EXIT
shape() { java -cp "$(lib_cp)" $C/Shape.java "$1" 2>/dev/null; }
num() { grep -oE "$1=[0-9a-z]+" <<<"$2" | cut -d= -f2; }
send() { $C/attack.sh "$1" | sed 's/^/        /'; }   # prints the measurement line; body in /tmp/attack.out
a=attacks
alive() { kill -0 "$($C/vulnerable.sh pid)" 2>/dev/null; }

# Brute force first: it needs a fresh rate-limit window.
before=$(curl -s localhost:4000/_ctf/stats | jq .graphqlRequests)
code=$(timeout 120 bash $a/brute-force.sh 2>/dev/null | tail -1)
used=$(( $(curl -s localhost:4000/_ctf/stats | jq .graphqlRequests) - before ))
[[ "$code" == 4821 ]] && (( used <= 12 )) && pass "brute-force.sh finds c1's invite code in $used HTTP requests" \
  || fail "brute-force.sh prints c1's invite code in 12 HTTP requests or fewer (used $used)" "the rate limit counts requests, not attempts"

s=$(shape $a/depth.graphql); send $a/depth.graphql
(( $(num depth "$s" || echo 0) >= 20 )) && jq -e '.data != null and (.errors | not)' /tmp/attack.out >/dev/null \
  && pass "depth.graphql: depth $(num depth "$s"), answered" || fail "depth.graphql: depth 20+ and answered with data (shape: ${s:-does not parse})"

s=$(shape $a/aliases.graphql); send $a/aliases.graphql
(( $(num maxSameField "$s" || echo 0) >= 100 )) && jq -e '.data != null and (.errors | not)' /tmp/attack.out >/dev/null \
  && pass "aliases.graphql: $(num maxSameField "$s") aliases of one field, answered" || fail "aliases.graphql: 100+ aliases of one field, answered (shape: ${s:-does not parse})"

send $a/fanout.graphql
n=$(jq -r '.extensions.debug.fieldFetches // 0' /tmp/attack.out 2>/dev/null || echo 0)
(( n >= 100000 )) && alive && pass "fanout.graphql: $n field fetches from one request, server still up" \
  || { alive && fail "fanout.graphql: 100,000+ field fetches (got $n)" "lists inside lists multiply" || { fail "fanout.graphql crashed the server: aim lower (512 MB heap)"; $C/vulnerable.sh start >/dev/null; }; }

s=$(shape $a/cycle.graphql); send $a/cycle.graphql
[[ "$(num cycle "$s")" == true ]] && grep -q 'Fragment cycles' /tmp/attack.out \
  && pass "cycle.graphql: a fragment cycle, refused at validation" || fail "cycle.graphql: a document whose fragments spread each other in a cycle"

send $a/suggestions.graphql
grep -q 'FieldUndefined' /tmp/attack.out && pass "suggestions.graphql: a near miss of a real field (record what the error says)" \
  || fail "suggestions.graphql: one misspelled field, so the server answers with FieldUndefined"

send $a/introspection.graphql
grep -q 'debugInfo' /tmp/attack.out && pass "introspection.graphql: finds the field the docs leave out" \
  || fail "introspection.graphql: the answer names the undocumented field" "deprecated fields are hidden unless you ask for them"

send $a/injection.graphql
jq -e '[.. | objects | select(has("id") and has("title")) | .id] | any(test("^b[0-9]+$") | not)' /tmp/attack.out >/dev/null 2>&1 \
  && pass "injection.graphql: rows from another table came back as books" || fail "injection.graphql: the answer holds a row that is not from the book table"

t=$(grep -cE '^# C[0-9]+' $a/csrf.hurl)
if (( t >= 3 )) && grep -q 'text/plain' $a/csrf.hurl && grep -qi 'cookie' $a/csrf.hurl \
   && hurl --test --variable "url=$URL" $a/csrf.hurl >/tmp/csrf.log 2>&1; then pass "csrf.hurl: $t entries, a text/plain mutation rides on the cookie"
else fail "csrf.hurl: 3+ entries, passing, with a text/plain request that carries only the cookie"; grep -E 'error|assert' /tmp/csrf.log | head -4 | sed 's/^/        /'; fi

notes_check notes.md 80 m10/03
finish
