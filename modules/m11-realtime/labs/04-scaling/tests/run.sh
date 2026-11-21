#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m11-realtime/contract/live-test.sh
cd "$LAB_DIR" || exit 1
app_build target/shelfie-live.jar || finish
trap 'ws_stop; [[ -n "$BAL_PID" ]] && kill $BAL_PID; replicas_stop' EXIT
T=$(mktemp -d)
run=$RANDOM   # a restarted Kafka consumer group may replay earlier runs' events: count only this run's
balancer 4000 4001,4002,4003 || { fail "the test's balancer did not start"; finish; }
for bus in redis kafka; do
  replicas_start target/shelfie-live.jar --shelfie.bus=$bus || { fail "$bus: three replicas start" "$(tail -3 /tmp/replica-1.log)"; continue; }
  room=r1; [[ $bus == kafka ]] && room=r2
  # Nine subscribers through the balancer: round-robin puts three on each replica.
  for i in $(seq 1 9); do ws_bg $T/$bus-$i "subscription { messageSent(roomId: \"$room\") { body } }" --seconds 20; done
  counts=$(for p in 4001 4002 4003; do gql "{\"query\":\"{ room(id: \\\"$room\\\") { watching } }\"}" http://localhost:$p/graphql | jq -r .data.room.watching; done | paste -sd,)
  [[ "$counts" == 3,3,3 ]] && pass "$bus: the balancer spread nine subscribers three per replica" \
    || fail "$bus: the balancer spread nine subscribers three per replica" "watching on a, b, c: $counts"
  # Three messages sent to each replica directly: every subscriber must get all nine.
  for p in 4001 4002 4003; do for k in 1 2 3; do send $room "Bot" "run $run via $p #$k" http://localhost:$p/graphql >/dev/null; done; done
  for i in $(seq 1 9); do ws_wait $T/$bus-$i 9 5; done; sleep 0.5
  want=$(for p in 4001 4002 4003; do for k in 1 2 3; do echo "run $run via $p #$k"; done; done | sort | paste -sd,)
  bad=0; worst=
  for i in $(seq 1 9); do
    have=$(nexts $T/$bus-$i | jq -r .data.messageSent.body | grep "^run $run " | sort | paste -sd,)
    [[ "$have" == "$want" ]] || { bad=$((bad + 1)); worst="subscriber $i got $(nexts $T/$bus-$i | grep -c "run $run ") of 9"; }
  done
  (( bad == 0 )) && pass "$bus: every one of 9 subscribers got all 9 messages, whichever replica they were sent to" \
    || fail "$bus: every one of 9 subscribers got all 9 messages, whichever replica they were sent to" "$bad subscribers missed some ($worst)"
  ws_stop; replicas_stop
done
notes_check notes.md 60 m11/04
finish
