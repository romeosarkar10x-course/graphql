#!/usr/bin/env bash
# Probe 5: rolling deploy with 200 open subscriptions. $1 = jar (default the reference app).
source /course/tools/testlib.sh; source /course/modules/m16-production/contract/ops.sh; source /course/modules/m16-production/contract/rollout.sh
L=/course/_workbench/2026-11-22-m16-probe/p5logs; mkdir -p $L
trap 'cp /tmp/rollout.log /tmp/subscribers.log /tmp/balancer.log /tmp/replica-*.log /tmp/reviews.log $L/ 2>/dev/null; rollout_cleanup; ops_stop' EXIT
( sleep 140; echo TIMEOUT; kill -TERM $$ ) &
rm -f /tmp/rollout.log
reviews_start || exit 1
rollout_run "${1:-/course/modules/m16-production/contract/ops-app/target/shelfie-ops.jar}" 200 25
echo "== balancer"; cat /tmp/balancer.log; echo "== rollout"; cat /tmp/rollout.log
grep -E 'REFUSING|completed every|Graceful|graceful' /tmp/replica-4001.log | cut -c1-160 | head
