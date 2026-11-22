# Module 18.6 helpers: run a ported reviews subgraph and hold it to the contract. Source after testlib.sh and
# Module 15's fed.sh.
#   port_start <dir> [port]     run <dir>/serve.sh <port> (default 4003; it builds, then serves), wait for /graphql
#   port_signatures <url>       the API as sorted one-line facts from introspection ("Review.author: User!"), for
#                               Query, Mutation, Review, Book and User; federation's own fields left out
#   port_graph <sdl file>       start Java's accounts, catalog and clubs, compose them with the port's SDL, start
#                               Apollo Router on :4000 (propagating X-Shelfie-User)
PORT_DIR=/course/modules/m18-frontier/contract/port
port_start() { # dir [port]
  local dir=$1 port=${2:-4003} i
  [[ -f $dir/serve.sh ]] || { echo "course: $dir/serve.sh is missing (it starts your subgraph on the port it is given)" >&2; return 1; }
  port_open $port && { echo "course: port $port is already in use" >&2; return 1; }
  (cd "$dir" && exec bash serve.sh $port) >/tmp/port.log 2>&1 &
  FED_PIDS+=($!)
  local pid=$!
  for i in $(seq 300); do curl -fs -o /dev/null -H 'content-type: application/json' -d '{"query":"{ __typename }"}' localhost:$port/graphql && return 0
    kill -0 $pid 2>/dev/null || break; sleep 0.5; done
  echo "course: the port did not answer on :$port/graphql: $(tail -3 /tmp/port.log | cut -c1-300)" >&2; return 1
}
port_signatures() { # url
  gql "$1" '{ __schema { types { name fields { name args { name type { ...T } defaultValue } type { ...T } } } } }
    fragment T on __Type { kind name ofType { kind name ofType { kind name ofType { kind name } } } }' |
  jq -r 'def t: if .kind == "NON_NULL" then (.ofType | t) + "!" elif .kind == "LIST" then "[" + (.ofType | t) + "]" else .name end;
    .data.__schema.types[] | select(.name | IN("Query", "Mutation", "Review", "Book", "User")) | .name as $o |
    .fields[] | select(.name | IN("_service", "_entities") | not) |
    "\($o).\(.name)" + (if (.args | length) > 0 then "(" + ([.args | sort_by(.name) | .[] | "\(.name): \(.type | t)" + (if .defaultValue then " = \(.defaultValue)" else "" end)] | join(", ")) + ")" else "" end) + ": \(.type | t)"' | sort
}
port_graph() { # sdl file
  local sdl=$1 w; w=$(dirname "$sdl")
  sub_start accounts 4001 && sub_start catalog 4002 && sub_start clubs 4004 || return 1
  sed -e "s#file: ../subgraphs/src/main/resources/graphql/reviews/reviews.graphqls#file: $sdl#" -e "s#file: \.\./subgraphs/#file: $FED/subgraphs/#" \
    $FED/compose/supergraph.yaml > $w/supergraph.yaml
  compose $w/supergraph.yaml $w/supergraph.graphql 2>$w/compose-errors.txt || { echo "course: composition failed: $(head -3 $w/compose-errors.txt | cut -c1-300)" >&2; return 2; }
  router_start apollo $w/supergraph.graphql $PORT_DIR/router.yaml
}
