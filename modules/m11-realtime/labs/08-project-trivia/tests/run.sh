#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m11-realtime/contract/live-test.sh
cd "$LAB_DIR" || exit 1
app_build target/shelfie-trivia.jar || finish
app_start java -jar target/shelfie-trivia.jar || finish
trap 'ws_stop; app_stop' EXIT
T=$(mktemp -d)
q() { gql "$(jq -nc --arg q "$1" '{query: $q}')"; }
answer() { q "mutation { answer(gameId: \"$G\", name: \"$1\", choice: $2) { accepted reason } }" | jq -r '.data.answer | if .accepted then "ok" else .reason end'; }
G=$(q 'mutation { createGame(questions: 3, secondsPerQuestion: 2) { id state } }' | jq -r '.data.createGame.id // empty')
[[ -n "$G" ]] || { fail "createGame returns a game id" "$(q 'mutation { createGame(questions: 3, secondsPerQuestion: 2) { id } }' | head -c 200)"; finish; }
for p in Ada Ben Cleo; do q "mutation { joinGame(gameId: \"$G\", name: \"$p\") { name score } }" >/dev/null; done
dup=$(q "mutation { joinGame(gameId: \"$G\", name: \"Ada\") { name } }" | jq -r '.errors[0].extensions.code')
[[ "$dup" == NAME_TAKEN ]] && pass "joinGame: a taken name is NAME_TAKEN" || fail "joinGame: a taken name is NAME_TAKEN" "got: $dup"
[[ "$(answer Ada 1)" == NO_QUESTION ]] && pass "answer before the start: NO_QUESTION" || fail "answer before the start: NO_QUESTION" "got: $(answer Ada 1)"
sel='__typename ... on QuestionAsked { question { index text choices } secondsToAnswer } ... on QuestionClosed { index correct answers } ... on LeaderboardChanged { leaderboard { name score } } ... on GameOver { leaderboard { name score } }'
ws_bg $T/spec "subscription { gameEvents(gameId: \"$G\") { $sel } }" --seconds 20
q "mutation { startGame(gameId: \"$G\") { state } }" >/dev/null
asked() { for _ in $(seq 1 60); do nexts $T/spec | jq -e --argjson i "$1" 'select(.data.gameEvents.__typename == "QuestionAsked" and .data.gameEvents.question.index == $i)' >/dev/null 2>&1 && return 0; sleep 0.1; done; return 1; }
replies=()
asked 0 && { replies+=("$(answer Ada 1)" "$(answer Ada 1)" "$(answer Ben 1)" "$(answer Cleo 3)"); }
asked 1 && { replies+=("$(answer Ada 1)" "$(answer Ben 1)" "$(answer Cleo 0)"); }
asked 2 && { replies+=("$(answer Ben 9)" "$(answer Ada 0)" "$(answer Ben 2)"); }   # Cleo never answers question 2
for _ in $(seq 1 40); do grep -q ' complete' $T/spec && break; sleep 0.1; done
[[ "${replies[*]}" == "ok ALREADY_ANSWERED ok ok ok ok ok BAD_CHOICE ok ok" ]] \
  && pass "answers: accepted once per question, ALREADY_ANSWERED and BAD_CHOICE refused" \
  || fail "answers: accepted once per question, ALREADY_ANSWERED and BAD_CHOICE refused" "got: ${replies[*]:-no question was asked}"
types=$(nexts $T/spec | jq -r .data.gameEvents.__typename | paste -sd,)
want=QuestionAsked,QuestionClosed,LeaderboardChanged,QuestionAsked,QuestionClosed,LeaderboardChanged,QuestionAsked,QuestionClosed,LeaderboardChanged,GameOver
[[ "$types" == "$want" ]] && grep -q ' complete' $T/spec \
  && pass "a spectator sees asked, closed, leaderboard for each question, then GameOver and complete" \
  || fail "a spectator sees asked, closed, leaderboard for each question, then GameOver and complete" "got: ${types:-nothing}"
closed=$(nexts $T/spec | jq -r 'select(.data.gameEvents.__typename == "QuestionClosed") | .data.gameEvents | "\(.index):\(.correct):\(.answers)"' | paste -sd,)
[[ "$closed" == "0:1:3,1:1:3,2:0:2" ]] && pass "QuestionClosed: index, correct choice and answer count (3, 3, then 2)" \
  || fail "QuestionClosed: index, correct choice and answer count (3, 3, then 2)" "got: $closed"
t_ask=$(awk '$2 == "next" && /QuestionAsked/ && /"index":2/ {print $1}' $T/spec); t_close=$(awk '$2 == "next" && /QuestionClosed/ && /"index":2/ {print $1}' $T/spec)
[[ -n "$t_close" && -n "$t_ask" ]] && (( t_close - t_ask >= 1700 )) && pass "question 2 waited for its 2 s timer because Cleo never answered ($((t_close - t_ask)) ms)" \
  || fail "question 2 waited for its 2 s timer because Cleo never answered" "closed $((t_close - t_ask)) ms after it was asked"
t_close1=$(awk '$2 == "next" && /QuestionClosed/ && /"index":0/ {print $1}' $T/spec); t_ask0=$(awk '$2 == "next" && /QuestionAsked/ && /"index":0/ {print $1}' $T/spec)
[[ -n "$t_close1" && -n "$t_ask0" ]] && (( t_close1 - t_ask0 < 1500 )) && pass "question 0 closed as soon as everyone had answered" \
  || fail "question 0 closed as soon as everyone had answered" "it closed $((t_close1 - t_ask0)) ms after it was asked"
final=$(nexts $T/spec | jq -r 'select(.data.gameEvents.__typename == "GameOver") | .data.gameEvents.leaderboard | map("\(.name)=\(.score)") | join(",")')
[[ "$final" == "Ada=450,Ben=200,Cleo=0" ]] && pass "scores: 100 a correct answer, 50 more for the first one (Ada 450, Ben 200, Cleo 0)" \
  || fail "scores: 100 a correct answer, 50 more for the first one (Ada 450, Ben 200, Cleo 0)" "GameOver leaderboard: ${final:-none}"
state=$(q "{ game(id: \"$G\") { state question { index } } }" | jq -c .data.game)
[[ "$state" == '{"state":"OVER","question":null}' ]] && pass "game(id): OVER, no open question" || fail "game(id): OVER, no open question" "got: $state"
late=$(q "mutation { joinGame(gameId: \"$G\", name: \"Dev\") { name } }" | jq -r '.errors[0].extensions.code')
[[ "$late" == ALREADY_STARTED ]] && pass "joinGame after the start: ALREADY_STARTED" || fail "joinGame after the start: ALREADY_STARTED" "got: $late"
finish
