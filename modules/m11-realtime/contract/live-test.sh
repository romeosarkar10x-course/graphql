# Shared by Module 11 tests: source it after testlib.sh and m06's app-test.sh.
#   gql <json body>                 POST to $URL, print the response
#   send <room> <author> <body>     sendMessage over HTTP, print the new message id
#   ws_bg <out file> <document> [Ws.java options]   start a subscriber in the background; wait until ready
#   ws_wait <out file> <n> [s]      wait up to s seconds (default 5) until <out file> has n "next" lines
#   nexts <out file>                print the payload of every "next" line, one per line
#   balancer <listen> <ports>       start contract/Balancer.java in the background, wait until it listens
LIVE=/course/modules/m11-realtime/contract
WS_PIDS=()
gql() { curl -s -H 'Content-Type: application/json' -d "$1" "${2:-$URL}"; }
send() {
  gql "$(jq -nc --arg r "$1" --arg a "$2" --arg b "$3" '{query: "mutation($r: ID!, $a: String!, $b: String!) { sendMessage(roomId: $r, author: $a, body: $b) { id } }", variables: {r: $r, a: $a, b: $b}}')" "${4:-$URL}" | jq -r '.data.sendMessage.id // empty'
}
ws_bg() {
  local out="$1" doc="$2"; shift 2
  : > "$out"
  java "$LIVE/Ws.java" "${WS_URL:-ws://localhost:$PORT/graphql}" "$doc" "$@" > "$out" 2>&1 & WS_PIDS+=($!)
  for _ in $(seq 1 100); do grep -qE ' (ready|closed|error)' "$out" && return 0; sleep 0.1; done
  return 1
}
ws_wait() { for _ in $(seq 1 $(( ${3:-5} * 10 ))); do (( $(grep -c ' next ' "$1") >= $2 )) && return 0; sleep 0.1; done; return 1; }
nexts() { grep ' next ' "$1" | sed 's/^ *[0-9]* next //'; }
ws_stop() { for p in "${WS_PIDS[@]}"; do kill "$p" 2>/dev/null; done; WS_PIDS=(); }
BAL_PID=
balancer() {
  java "$LIVE/Balancer.java" "$1" "$2" > /tmp/balancer.log 2>&1 & BAL_PID=$!
  for _ in $(seq 1 100); do (exec 3<>/dev/tcp/localhost/$1) 2>/dev/null && return 0; sleep 0.1; done
  return 1
}
# Lab 11.4 on: three instances of the app under test on :4001-4003, named a, b, c.
#   replicas_start <jar> [app args...]   start them, wait until all listen (and, with Kafka, have partitions)
#   replicas_stop
REPLICA_PIDS=()
replicas_start() {
  local jar="$1" i port; shift
  for i in 1 2 3; do
    port=$((4000 + i))
    java -Xmx256m -jar "$jar" --server.port=$port --shelfie.instance=$(printf "\\x$(printf %x $((96 + i)))") "$@" > /tmp/replica-$i.log 2>&1 &
    REPLICA_PIDS+=($!)
  done
  for i in 1 2 3; do
    port=$((4000 + i))
    for _ in $(seq 1 120); do (exec 3<>/dev/tcp/localhost/$port) 2>/dev/null && break; kill -0 ${REPLICA_PIDS[$((i-1))]} 2>/dev/null || break; sleep 0.5; done
    (exec 3<>/dev/tcp/localhost/$port) 2>/dev/null || { echo "replica $i did not start: $(grep -m2 -E 'ERROR|Caused by' /tmp/replica-$i.log)"; return 1; }
  done
  if [[ " $* " == *"bus=kafka"* ]]; then
    for i in 1 2 3; do for _ in $(seq 1 60); do grep -q 'partitions assigned' /tmp/replica-$i.log && break; sleep 0.5; done; done
  fi
  return 0
}
replicas_stop() { for p in "${REPLICA_PIDS[@]}"; do kill "$p" 2>/dev/null; done; for p in "${REPLICA_PIDS[@]}"; do wait "$p" 2>/dev/null; done; REPLICA_PIDS=(); }
