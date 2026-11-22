#!/usr/bin/env bash
# Lab 15.7: apply states/1, states/2, ... in order while traffic flows. Each step is one team's deploy: exactly one
# subgraph's SDL changes. A changed subgraph is deployed blue/green (a new instance on another port, the supergraph
# re-pointed, the old instance stopped); the router reloads the supergraph file. Accounts and clubs never change.
source /course/tools/testlib.sh
source /course/modules/m15-federation/contract/fed.sh
cd "$LAB_DIR" || exit 1
W=$(mktemp -d); trap 'touch $W/stop; rm -rf $W; fed_stop' EXIT
G=$FED/subgraphs/src/main/resources/graphql
states=$(ls -d states/[0-9]* 2>/dev/null | sort -t/ -k2 -n)
n=$(echo "$states" | grep -c .)
(( n >= 2 )) || { fail "states/1 and at least one more state" "found: $(echo $states)"; finish; }
cmp -s states/1/catalog.graphqls $LAB_HOME/starter/states/1/catalog.graphqls && cmp -s states/1/reviews.graphqls $LAB_HOME/starter/states/1/reviews.graphqls \
  && pass "states/1 is production as it is today (unchanged)" || fail "states/1 is production as it is today" "states/1 must stay as given: it is what runs now"
declare -A port=([catalog]=4002 [reviews]=4003) pid=()
yaml() { # state dir -> supergraph.yaml on stdout
  printf 'subgraphs:\n'
  printf '  accounts:\n    routing_url: http://localhost:4001/graphql\n    schema:\n      file: %s\n' $G/accounts/accounts.graphqls
  printf '  catalog:\n    routing_url: http://localhost:%s/graphql\n    schema:\n      file: %s\n' ${port[catalog]} $PWD/$1/catalog.graphqls
  printf '  reviews:\n    routing_url: http://localhost:%s/graphql\n    schema:\n      file: %s\n' ${port[reviews]} $PWD/$1/reviews.graphqls
  printf '  clubs:\n    routing_url: http://localhost:4004/graphql\n    schema:\n      file: %s\n' $G/clubs/clubs.graphqls
}
deploy() { # subgraph state-dir port -> starts it, prints pid
  local name=$1 dir=$W/schemas/$2/$1 extra=()
  mkdir -p $dir && cp $2/$1.graphqls $dir/
  [[ $name == catalog ]] && grep -qE '^type Review\b' $dir/catalog.graphqls && extra=(--shelfie.catalog.review-book=true)
  sub_start $name $3 --spring.graphql.schema.locations=file:$dir/ "${extra[@]}" || return 1
  pid[$name]=${FED_PIDS[-1]}
}
# Every state must compose before anything starts.
ok=1
for s in $states; do
  for f in catalog reviews; do [[ -f $s/$f.graphqls ]] || { fail "$s has $f.graphqls and reviews.graphqls"; ok=0; }; done
  yaml $s > $W/check.yaml
  compose $W/check.yaml $W/check.graphql 2>$W/err.txt && pass "$s composes" || { fail "$s composes" "$(head -2 $W/err.txt | cut -c1-250)"; ok=0; }
done
(( ok )) || finish
prev=; for s in $states; do
  if [[ -n $prev ]]; then
    changed=$(for f in catalog reviews; do cmp -s $prev/$f.graphqls $s/$f.graphqls || echo $f; done | paste -sd' ')
    [[ $(wc -w <<<"$changed") == 1 ]] && pass "$prev to $s: one team deploys ($changed)" \
      || { fail "$prev to $s: exactly one subgraph changes per step" "changed: ${changed:-nothing}"; ok=0; }
  fi; prev=$s
done
last=$(echo "$states" | tail -1)
if grep -qE '^type Review\b' $last/catalog.graphqls; then fail "at the end catalog no longer declares Review" "$last/catalog.graphqls still has type Review"; ok=0
else pass "at the end catalog no longer declares Review"; fi
if grep -E '^\s*book: Book!' $last/reviews.graphqls | grep -q '@override'; then fail "at the end Review.book is plain in reviews" "$last still has @override: remove it once catalog is done"; ok=0
elif grep -qE '^\s*book: Book!' $last/reviews.graphqls; then pass "at the end reviews owns Review.book, no @override left"
else fail "at the end reviews owns Review.book" "no 'book: Book!' in $last/reviews.graphqls"; ok=0; fi
(( ok )) || finish
# Production today.
sub_start accounts 4001 && sub_start clubs 4004 && deploy catalog states/1 4002 && deploy reviews states/1 4003 || { fail "state 1 starts"; finish; }
yaml states/1 > $W/sg.yaml && compose $W/sg.yaml $W/supergraph.graphql
HOT_RELOAD=1 router_start apollo $W/supergraph.graphql || { fail "router starts"; finish; }
# Traffic: two clients that read Review.book, every 50 ms, until told to stop.
( while [[ ! -f $W/stop ]]; do
    for q in '{ latestReviews(first: 5) { id book { title } } }' '{ review(id: "r1") { rating book { title } } }'; do
      r=$(gql http://127.0.0.1:4000/ "$q")
      if [[ $(jq -r '(.errors|length) == 0 and ([.. | objects | select(has("book")) | .book.title] | all(. != null))' <<<"$r" 2>/dev/null) == true ]]
      then echo ok >> $W/traffic; else echo "$(date +%T) $r" | cut -c1-240 >> $W/traffic.bad; echo bad >> $W/traffic; fi
    done; sleep 0.05
  done ) &
sleep 2
prev=states/1
for s in $(echo "$states" | tail -n +2); do
  for f in catalog reviews; do
    cmp -s $prev/$f.graphqls $s/$f.graphqls && continue
    old=${pid[$f]}; new_port=$(( port[$f] < 4010 ? port[$f] + 10 : port[$f] - 10 ))
    deploy $f $s $new_port || { fail "$s: $f deploys" "$(grep -m2 -E 'Caused by|ERROR' /tmp/sub-$f.log | cut -c1-250)"; break 2; }
    port[$f]=$new_port
    yaml $s > $W/sg.yaml && compose $W/sg.yaml $W/next.graphql && mv $W/next.graphql $W/supergraph.graphql
    sleep 4                          # the router notices the file and swaps the supergraph
    kill $old; wait $old 2>/dev/null
    sleep 1
  done
  note "$s applied ($(grep -c . $W/traffic) requests so far, $(grep -c bad $W/traffic) failed)"
  prev=$s
done
sleep 2; touch $W/stop; sleep 1
total=$(grep -c . $W/traffic); bad=$(grep -c bad $W/traffic)
if (( total > 50 && bad == 0 )); then pass "zero errors across the migration ($total requests)"
else fail "zero errors across the migration" "$bad of $total requests failed; first: $(head -1 $W/traffic.bad 2>/dev/null)"; fi
r=$(gql http://127.0.0.1:4000/ '{ review(id: "r1") { book { title } } }' '{}' 'Apollo-Expose-Query-Plan: true')
plan=$(jq -r .extensions.apolloQueryPlan.text <<<"$r")
grep -q '\.\.\. on Book' <<<"$plan" && ! grep -q '\.\.\. on Review' <<<"$plan" && [[ $(jq -r .data.review.book.title <<<"$r") != null ]] \
  && pass "Review.book now comes from reviews (catalog only adds the title)" || fail "Review.book comes from reviews at the end"
require_file RUNBOOK.md "the migration runbook (rubric)" && notes_check RUNBOOK.md 150 m15/07
finish
