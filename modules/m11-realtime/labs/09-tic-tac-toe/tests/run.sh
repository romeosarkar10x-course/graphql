#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m11-realtime/contract/live-test.sh
cd "$LAB_DIR" || exit 1
app_build target/shelfie-ttt.jar || finish
app_start java -jar target/shelfie-ttt.jar || finish
trap 'ws_stop; app_stop' EXIT
T=$(mktemp -d)
q() { gql "$(jq -nc --arg q "$1" '{query: $q}')"; }
mv() { q "mutation { move(id: \"$1\", name: \"$2\", cell: $3) { moves } }" | jq -r 'if .errors then .errors[0].extensions.code // "error" else "ok" end'; }
join() { q "mutation { joinMatch(id: \"$1\", name: \"$2\") }" | jq -r '.data.joinMatch // .errors[0].extensions.code'; }
M=$(q 'mutation { newMatch { id } }' | jq -r '.data.newMatch.id // empty')
[[ -n "$M" ]] || { fail "newMatch returns a match id"; finish; }
roles="$(join $M Ada) $(join $M Ben) $(join $M Cleo)"
[[ "$roles" == "X O MATCH_FULL" ]] && pass "joinMatch: Ada plays X, Ben O, a third player is MATCH_FULL" \
  || fail "joinMatch: Ada plays X, Ben O, a third player is MATCH_FULL" "got: $roles"
W=$(q 'mutation { newMatch { id } }' | jq -r .data.newMatch.id); join $W Ada >/dev/null
[[ "$(mv $W Ada 0)" == WAITING_FOR_OPPONENT ]] && pass "a move before O has joined: WAITING_FOR_OPPONENT" \
  || fail "a move before O has joined: WAITING_FOR_OPPONENT" "got: $(mv $W Ada 0)"
sel='board next winner line draw spectators moves'
ws_bg $T/ada "subscription { matchUpdated(id: \"$M\", as: \"Ada\") { $sel } }" --seconds 15
ws_bg $T/spec "subscription { matchUpdated(id: \"$M\") { $sel } }" --seconds 15
sleep 0.5
specs=$(q "{ match(id: \"$M\") { spectators } }" | jq -r .data.match.spectators)
[[ "$specs" == 1 ]] && pass "spectators: one spectator counted, the player's own subscription not" \
  || fail "spectators: one spectator counted, the player's own subscription not" "spectators: $specs"
first=$(nexts $T/spec | head -1 | jq -c '.data.matchUpdated | {board, next}')
[[ "$first" == '{"board":["","","","","","","","",""],"next":"X"}' ]] && pass "a new subscriber gets the current board at once" \
  || fail "a new subscriber gets the current board at once" "first event: $first"
r=()
for m in "Ben 4" "Ada 0" "Ada 1" "Ben 0" "Ben 9" "Cleo 2" "Ben 3" "Ada 4" "Ben 5" "Ada 8" "Ben 6"; do r+=("$(mv $M $m)"); done
[[ "${r[*]}" == "NOT_YOUR_TURN ok NOT_YOUR_TURN CELL_TAKEN BAD_CELL NOT_A_PLAYER ok ok ok ok GAME_OVER" ]] \
  && pass "moves: turn order, taken and bad cells, outsiders and moves after the end are refused with their codes" \
  || fail "moves: turn order, taken and bad cells, outsiders and moves after the end are refused with their codes" "got: ${r[*]}"
for _ in $(seq 1 30); do grep -q ' complete' $T/spec && break; sleep 0.1; done
last=$(nexts $T/spec | tail -1 | jq -c '.data.matchUpdated | {board, next, winner, line, draw, moves}')
[[ "$last" == '{"board":["X","","","O","X","O","","","X"],"next":null,"winner":"X","line":[0,4,8],"draw":false,"moves":5}' ]] \
  && pass "the last update: X wins on 0, 4, 8, nobody is next" || fail "the last update: X wins on 0, 4, 8, nobody is next" "got: $last"
boards=$(nexts $T/spec | jq -r '.data.matchUpdated.moves' | paste -sd,)
[[ "$boards" == *"1,2,3,4,5" ]] && pass "the spectator saw every move in order ($boards)" || fail "the spectator saw every move in order" "moves seen: $boards"
grep -q ' complete' $T/spec && grep -q ' complete' $T/ada && pass "both subscriptions complete when the match ends" \
  || fail "both subscriptions complete when the match ends" "spectator: $(tail -1 $T/spec | cut -c1-30); Ada: $(tail -1 $T/ada | cut -c1-30)"
ws_stop; sleep 0.5
ws_bg $T/late "subscription { matchUpdated(id: \"$M\") { winner } }" --seconds 3; sleep 0.8
[[ "$(nexts $T/late | jq -r .data.matchUpdated.winner)" == X ]] && grep -q ' complete' $T/late \
  && pass "a spectator arriving after the end gets the final board and complete" \
  || fail "a spectator arriving after the end gets the final board and complete" "got: $(cut -c1-60 $T/late | paste -sd'|')"
D=$(q 'mutation { newMatch { id } }' | jq -r .data.newMatch.id); join $D Ada >/dev/null; join $D Ben >/dev/null
for m in "Ada 0" "Ben 1" "Ada 2" "Ben 4" "Ada 3" "Ben 5" "Ada 7" "Ben 6" "Ada 8"; do mv $D $m >/dev/null; done
draw=$(q "{ match(id: \"$D\") { winner draw next moves } }" | jq -c .data.match)
[[ "$draw" == '{"winner":null,"draw":true,"next":null,"moves":9}' ]] && pass "a full board with no line is a draw" \
  || fail "a full board with no line is a draw" "got: $draw"
specs=$(q "{ match(id: \"$W\") { spectators } }" | jq -r .data.match.spectators)
ws_bg $T/w "subscription { matchUpdated(id: \"$W\") { moves } }" --seconds 30; sleep 0.4; ws_stop; sleep 1
after=$(q "{ match(id: \"$W\") { spectators } }" | jq -r .data.match.spectators)
[[ "$specs" == 0 && "$after" == 0 ]] && pass "a spectator who disconnects is no longer counted" \
  || fail "a spectator who disconnects is no longer counted" "before: $specs, after leaving: $after"
finish
