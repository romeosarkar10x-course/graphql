# Module 15 helpers for tests and experiments, inside the course image. Source after tools/testlib.sh.
#   sub_jar                         build (once) and print the contract subgraphs jar
#   sub_start <name> <port> [args]  start one subgraph (accounts|catalog|reviews|clubs) from a jar ($SUB_JAR, or
#                                   the contract's), wait until /_stats answers
#   subs_start [args]               the four contract subgraphs on 4001-4004
#   sub_stats <port>                its /_stats JSON;  sub_reset <port>
#   compose <supergraph.yaml> <out> [api]  compose with The Guild's composition library (MIT); errors on stderr
#   router_start apollo|hive <supergraph> [config] [port]  start a router (default port 4000), wait for health;
#                                   HOT_RELOAD=1 makes Apollo Router reload the supergraph file when it changes;
#                                   ROUTER_HOST=0.0.0.0 listens beyond the container (default 127.0.0.1)
#   gql <url> <query> [variables] [headers...]  POST a GraphQL request, print the JSON
#   plan_steps apollo|hive <url> <query>  the router's plan for a query as {"steps": [[...]], "fetches": n}
#   fed_stop                        stop everything started here (also on exit)
FED=/course/modules/m15-federation/contract
FED_BIN=/course/.cache/fed/bin
FED_PIDS=()
fed_stop() { local p; for p in "${FED_PIDS[@]}"; do kill "$p" 2>/dev/null; done; for p in "${FED_PIDS[@]}"; do wait "$p" 2>/dev/null; done; FED_PIDS=(); return 0; }
trap fed_stop EXIT

sub_jar() {
  local jar=$FED/subgraphs/target/shelfie-subgraphs.jar
  [[ -f $jar ]] || (cd $FED/subgraphs && mvn -q package -DskipTests >/tmp/subgraphs-build.log 2>&1) || { echo "course: contract subgraphs did not build (/tmp/subgraphs-build.log)" >&2; return 1; }
  echo $jar
}
port_open() { (exec 3<>/dev/tcp/localhost/$1) 2>/dev/null; }
sub_start() { # name port [extra args]
  local name=$1 port=$2 jar=${SUB_JAR:-$(sub_jar)}; shift 2
  port_open $port && { echo "course: port $port is already in use" >&2; return 1; }
  java -XX:TieredStopAtLevel=1 -Xmx192m -jar "$jar" --spring.profiles.active=$name --server.port=$port "$@" >/tmp/sub-$name.log 2>&1 &
  FED_PIDS+=($!)
  local pid=$! i
  for i in $(seq 120); do curl -fs -o /dev/null localhost:$port/_stats && return 0; kill -0 $pid 2>/dev/null || break; sleep 0.5; done
  echo "course: subgraph $name did not start on $port: $(grep -m2 -E 'Caused by|ERROR' /tmp/sub-$name.log | cut -c1-300)" >&2; return 1
}
subs_start() { sub_start accounts 4001 "$@" && sub_start catalog 4002 "$@" && sub_start reviews 4003 "$@" && sub_start clubs 4004 "$@"; }
sub_stats() { curl -fs localhost:$1/_stats; }
sub_reset() { curl -fs -o /dev/null -X POST localhost:$1/_reset; }

compose() { # supergraph.yaml out [api]
  [[ -d $FED/compose/node_modules ]] || (cd $FED/compose && npm ci --no-audit --no-fund >/tmp/compose-install.log 2>&1) || { echo "course: npm ci failed in contract/compose" >&2; return 1; }
  NODE_PATH=$FED/compose/node_modules node $FED/compose/compose.mjs "$@"
}

router_start() { # apollo|hive supergraph [config] [port]
  local kind=$1 sg=$2 cfg=${3:-} port=${4:-4000} health i
  port_open $port && { echo "course: port $port is already in use" >&2; return 1; }
  case $kind in
    apollo) [[ -n $cfg ]] || cfg=$FED/routers/apollo.yaml
            $FED_BIN/router --config "$cfg" --supergraph "$sg" --listen ${ROUTER_HOST:-127.0.0.1}:$port ${HOT_RELOAD:+--hot-reload} >/tmp/router-apollo.log 2>&1 &
            health=http://127.0.0.1:8088/health ;;
    hive)   [[ -n $cfg ]] || cfg=$FED/routers/hive.yaml
            env ROUTER_CONFIG_FILE_PATH="$cfg" SUPERGRAPH_FILE_PATH="$sg" PORT=$port HOST=${ROUTER_HOST:-127.0.0.1} $FED_BIN/hive-router >/tmp/router-hive.log 2>&1 &
            health=http://127.0.0.1:$port/health ;;
    *) echo "course: router_start apollo|hive" >&2; return 1 ;;
  esac
  FED_PIDS+=($!)
  local pid=$!
  for i in $(seq 60); do curl -fs -o /dev/null $health && port_open $port && return 0; kill -0 $pid 2>/dev/null || break; sleep 0.5; done
  echo "course: $kind router did not start: $(tail -3 /tmp/router-$kind.log | cut -c1-300)" >&2; return 1
}

gql() { # url query [variables] [header...]
  local url=$1 q=$2 v=${3:-'{}'}; shift 3 2>/dev/null || shift $#
  local h=() x; for x in "$@"; do h+=(-H "$x"); done
  jq -n --arg q "$q" --argjson v "$v" '{query:$q, variables:$v}' | curl -s "$url" -H 'content-type: application/json' "${h[@]}" --data-binary @-
}

plan_steps() { # apollo|hive url query
  local h; [[ $1 == apollo ]] && h='Apollo-Expose-Query-Plan: true' || h='hive-expose-query-plan: true'
  local f; f=$(mktemp)
  gql "$2" "$3" '{}' "$h" > "$f"
  java -cp "$(lib_cp)" $FED/PlanSteps.java "$f"
  rm -f "$f"
}
