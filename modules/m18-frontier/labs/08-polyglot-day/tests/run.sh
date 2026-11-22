#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m15-federation/contract/fed.sh
source /course/modules/m18-frontier/contract/port/port.sh
cd "$LAB_DIR" || exit 1
W=$(mktemp -d); trap 'rm -rf $W; fed_stop' EXIT
T=team.json
jq -e '.subgraphs | length >= 4' $T >/dev/null 2>&1 || { fail "team.json lists the subgraphs (at least accounts, catalog, reviews, clubs)"; finish; }
for need in accounts catalog reviews clubs; do jq -e --arg n $need '.subgraphs | any(.name == $n)' $T >/dev/null || { fail "team.json has no $need subgraph"; finish; }; done
declare -A PORTS=([accounts]=4001 [catalog]=4002 [reviews]=4003 [clubs]=4004); next=4005
yaml="federation_version: =2.9.0"$'\n'"subgraphs:"
n=$(jq '.subgraphs | length' $T)
for i in $(seq 0 $((n - 1))); do
  s=$(jq -c ".subgraphs[$i]" $T); name=$(jq -r .name <<<"$s"); lang=$(jq -r '.language // "?"' <<<"$s")
  port=${PORTS[$name]:-$next}; [[ -n ${PORTS[$name]:-} ]] || next=$((next + 1))
  url=$(jq -r '.url // empty' <<<"$s")
  if [[ -n $url ]]; then :   # a friend's subgraph (group variant), already running
  else
    run=$(jq -r '.run // "contract"' <<<"$s"); dir=/course/$(jq -r '.dir // ""' <<<"$s")
    case $run in
      contract) sub_start $name $port || { fail "the contract's $name subgraph starts"; finish; } ;;
      serve.sh) port_start "$dir" $port || { fail "$name ($lang): $dir/serve.sh starts it on :$port" "$(tail -3 /tmp/port.log | cut -c1-200)"; finish; } ;;
      jar:*)    jar=${run#jar:}; (cd "$dir" && mvn -q package -DskipTests >/tmp/build-$name.log 2>&1) || { fail "$name ($lang): mvn package in $dir"; finish; }
                java -jar "$dir/$jar" $port >/tmp/sub-$name.log 2>&1 & FED_PIDS+=($!)
                for _ in $(seq 120); do port_open $port && break; sleep 0.5; done ;;
      *) fail "team.json: $name has an unknown run \"$run\" (contract, serve.sh, jar:<path>)"; finish ;;
    esac
    url=http://localhost:$port/graphql
  fi
  gql $url '{ _service { sdl } }' | jq -r '.data._service.sdl // empty' > $W/$name.graphqls
  [[ -s $W/$name.graphqls ]] || { fail "$name ($lang) answers { _service { sdl } } at $url"; finish; }
  yaml+=$'\n'"  $name:"$'\n'"    routing_url: $url"$'\n'"    schema:"$'\n'"      file: $W/$name.graphqls"
  echo "$lang" >> $W/languages.txt
done
pass "all $n subgraphs answer { _service { sdl } }"
echo "$yaml" > $W/supergraph.yaml
compose $W/supergraph.yaml $W/supergraph.graphql 2>$W/errors.txt && pass "the $n subgraphs compose into one supergraph" || { fail "composition" "$(head -3 $W/errors.txt | cut -c1-300)"; finish; }
router_start apollo $W/supergraph.graphql $PORT_DIR/router.yaml || { fail "Apollo Router starts"; finish; }
URL=http://127.0.0.1:4000/
app_hurl $LAB_HOME/tests/polyglot.hurl "one graph across languages (polyglot.hurl)" --variable router=$URL
langs=$(sort -fu $W/languages.txt | paste -sd, - | sed 's/,/, /g'); count=$(sort -fu $W/languages.txt | wc -l)
echo "        leaderboard: $count language(s): $langs"
(( count >= 3 )) && pass "$count languages in one supergraph ($langs)" || fail "$count language(s) in the supergraph: the solo target is 3 (Java, plus your Kotlin and TypeScript ports)"
finish
