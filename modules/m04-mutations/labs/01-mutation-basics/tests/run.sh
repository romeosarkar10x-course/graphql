#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR"
require_file FirstMutations.java || finish
ref_reset_data; ref_reset_stats
timeout 60 java -cp "$(lib_cp)" FirstMutations.java >/tmp/fm.txt 2>/tmp/fm.err
if diff -q /tmp/fm.txt "$LAB_HOME/tests/expected/first-mutations.txt" >/dev/null; then pass "four mutations, four lines, all from the payloads"
else fail "output differs (< yours, > expected)"; diff /tmp/fm.txt "$LAB_HOME/tests/expected/first-mutations.txt" | sed 's/^/        /' | head -10; tail -2 /tmp/fm.err | sed 's/^/        /'; fi
n=$(ref_stats | jq .graphql)
[[ "$n" == 4 ]] && pass "exactly 4 GraphQL requests: data selected back, no follow-up queries" || fail "expected 4 GraphQL requests, the server counted $n"
state=$(gql_as "" '{ club(id: "c2") { memberCount } book(id: "b2") { averageRating reviews { id } } }')
jq -e '.data.club.memberCount == 4 and .data.book.averageRating == 4.5 and (.data.book.reviews | length) == 2' <<<"$state" >/dev/null \
  && pass "server state is back to the seed afterwards" || fail "the script should leave c2 and b2 as it found them" "$(jq -c .data <<<"$state")"
ref_reset_data

require_file SerialVsParallel.java || finish
timeout 60 java -cp "$(lib_cp)" SerialVsParallel.java >/tmp/svp.txt 2>/tmp/svp.err
qt=$(awk '$1=="query" && $2=="total"{print $3}' /tmp/svp.txt); mt=$(awk '$1=="mutation" && $2=="total"{print $3}' /tmp/svp.txt)
qs=$(awk '$1=="query" && $3=="start"{print $4}' /tmp/svp.txt | sort -n | tr '\n' ' ')
if [[ -n "$qt" && -n "$mt" ]]; then
  (( qt < 450 )) && pass "query: three 200 ms fields overlap (total ${qt} ms)" || fail "query took ${qt} ms: its fields did not run in parallel" "starts: $qs"
  (( mt >= 600 )) && pass "mutation: three 200 ms fields run one after another (total ${mt} ms)" || fail "mutation took ${mt} ms: root mutation fields must be serial"
  ok=$(awk '$1=="mutation" && $3=="start"{s[$2]=$4; e[$2]=$6} END{print (s["b"]>=e["a"] && s["c"]>=e["b"]) ? 1 : 0}' /tmp/svp.txt)
  [[ "$ok" == 1 ]] && pass "mutation: b starts after a ends, c after b" || fail "mutation fields overlapped"
else fail "SerialVsParallel.java should print 'query total <ms>' and 'mutation total <ms>'" "$(tail -2 /tmp/svp.err)"; fi
notes_check notes.md 60 m04/01
finish
