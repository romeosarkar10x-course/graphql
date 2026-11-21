#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m11-realtime/contract/live-test.sh
cd "$LAB_DIR" || exit 1
app_build target/shelfie-live.jar || finish
CHATS=()
trap 'for c in "${CHATS[@]}"; do kill $c 2>/dev/null; done; [[ -n "$BAL_PID" ]] && kill $BAL_PID; replicas_stop' EXIT
T=$(mktemp -d)
# Empty the game's Redis database (11) so ids and history start fresh.
exec 3<>/dev/tcp/redis/6379 && printf '*2\r\n$6\r\nSELECT\r\n$2\r\n11\r\n*1\r\n$7\r\nFLUSHDB\r\n' >&3 && head -c 10 <&3 >/dev/null; exec 3>&-
replicas_start target/shelfie-live.jar --shelfie.bus=redis || { fail "three replicas start" "$(tail -3 /tmp/replica-1.log)"; finish; }
balancer 4000 4001,4002,4003 || { fail "the test's balancer did not start"; finish; }
for i in $(seq 1 9); do
  java -cp "$(lib_cp)" client/Chat.java http://localhost:4000/graphql r1 --seconds 22 > $T/c$i.out 2> $T/c$i.err & CHATS+=($!)
done
for i in $(seq 1 9); do for _ in $(seq 1 50); do grep -q '^connected' $T/c$i.err && break; sleep 0.1; done; done
sleep 0.5
# One sender, 48 messages every 250 ms, to the replicas in turn; a message to a dead replica goes to the next.
sent=(); ports=(4001 4002 4003)
restart() { java -Xmx256m -jar target/shelfie-live.jar --server.port=$1 --shelfie.instance=$2 --shelfie.bus=redis > /tmp/replica-$2.log 2>&1 & REPLICA_PIDS+=($!); }
for k in $(seq 1 48); do
  case $k in
    12) kill -9 ${REPLICA_PIDS[1]} ;;               # the instructor kills b...
    24) restart 4002 b2 ;;                          # ...brings it back...
    30) kill -9 ${REPLICA_PIDS[2]} ;;               # ...and kills c
  esac
  for try in 0 1 2; do
    p=${ports[$(( (k + try) % 3 ))]}
    id=$(send r1 Teacher "message $k" http://localhost:$p/graphql 2>/dev/null)
    [[ -n "$id" ]] && { sent+=("$id"); break; }
  done
  sleep 0.25
done
sleep 4
for c in "${CHATS[@]}"; do kill $c 2>/dev/null; wait $c 2>/dev/null; done; CHATS=()
(( ${#sent[@]} == 48 )) && pass "the sender got all 48 messages accepted, retrying around dead replicas" \
  || fail "the sender got all 48 messages accepted, retrying around dead replicas" "accepted: ${#sent[@]}"
want=$(printf '%s\n' "${sent[@]}" | paste -sd,)
ok=0; dropped=0; report=
for i in $(seq 1 9); do
  have=$(grep 'Teacher: message' $T/c$i.out | cut -d' ' -f1 | paste -sd,)
  grep -q '^dropped' $T/c$i.err && dropped=$((dropped + 1))
  if [[ "$have" == "$want" ]]; then ok=$((ok + 1)); else
    n=$(grep -c 'Teacher: message' $T/c$i.out); u=$(grep 'Teacher: message' $T/c$i.out | cut -d' ' -f1 | sort -u | wc -l)
    report+="client $i: $n lines, $u distinct of ${#sent[@]}; "
  fi
done
(( dropped >= 2 )) && pass "the kills cut off clients ($dropped of 9 had to reconnect)" \
  || fail "the kills cut off clients" "only $dropped of 9 clients saw a drop: was the load spread?"
(( ok == 9 )) && pass "every client printed all 48 messages exactly once, in order" \
  || fail "every client printed all 48 messages exactly once, in order" "$ok of 9 did. ${report}"
[[ -n "$KEEP" ]] && { mkdir -p "$KEEP"; cp $T/* /tmp/replica-*.log "$KEEP"/; printf "%s\n" "${sent[@]}" > "$KEEP/sent"; }
finish
