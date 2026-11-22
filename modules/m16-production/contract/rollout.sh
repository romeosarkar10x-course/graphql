#!/usr/bin/env bash
# Module 16.4's rolling deploy, inside the course image. Source ops.sh first; uses the reviews service on :4100.
#   rollout_run <jar> [clients] [seconds]     prints the Subscribers JSON line; balancer log in /tmp/balancer.log
# Two replicas of <jar> on :4001 and :4002 behind ReadyBalancer on :4000 (readiness-aware). While <clients>
# subscribers listen and a writer adds a review every 50 ms, each replica in turn gets SIGTERM, is waited for,
# started again and waited for until ready (a rolling deploy of the same version).
ROLL_PIDS=()
replica_start() {   # <jar> <port>: start one replica, wait until its readiness answers 200 (60 s)
  java -jar "$1" --server.port="$2" >"/tmp/replica-$2.log" 2>&1 & ROLL_PIDS[$2]=$!
  for _ in $(seq 1 120); do
    [[ "$(curl -s -o /dev/null -w '%{http_code}' "localhost:$2/actuator/health/readiness")" == 200 ]] && return 0
    kill -0 "${ROLL_PIDS[$2]}" 2>/dev/null || break; sleep 0.5
  done
  return 1
}
replica_stop() { kill -TERM "${ROLL_PIDS[$1]}" 2>/dev/null; wait "${ROLL_PIDS[$1]}" 2>/dev/null; }
rollout_cleanup() { local p; for p in "${ROLL_PIDS[@]}" $BAL_PID; do kill "$p" 2>/dev/null; wait "$p" 2>/dev/null; done; return 0; }
rollout_run() {
  local jar=$1 clients=${2:-200} seconds=${3:-30} sub
  java "$OPS/ReadyBalancer.java" 4000 4001,4002 >/tmp/balancer.log 2>&1 & BAL_PID=$!
  replica_start "$jar" 4001 && replica_start "$jar" 4002 || { echo '{"error":"a replica did not become ready"}'; rollout_cleanup; return 1; }
  sleep 0.5
  java "$OPS/Subscribers.java" --clients "$clients" --seconds "$seconds" >/tmp/subscribers.log 2>&1 & sub=$!
  for _ in $(seq 1 120); do grep -q 'all subscribed' /tmp/subscribers.log && break; sleep 0.5; done
  grep -q 'all subscribed' /tmp/subscribers.log || { echo '{"error":"the subscribers never all subscribed"}'; kill $sub; rollout_cleanup; return 1; }
  sleep 3
  for port in 4001 4002; do
    echo "$(date +%s%3N) stop $port" >>/tmp/rollout.log; replica_stop $port
    echo "$(date +%s%3N) start $port" >>/tmp/rollout.log; replica_start "$jar" $port || { echo '{"error":"a replica did not come back"}'; kill $sub; rollout_cleanup; return 1; }
    echo "$(date +%s%3N) ready $port" >>/tmp/rollout.log; sleep 3
  done
  wait $sub
  rollout_cleanup   # rollout_run often runs in $(...): stop the balancer and replicas here, not in the caller's trap
  tail -1 /tmp/subscribers.log
}
