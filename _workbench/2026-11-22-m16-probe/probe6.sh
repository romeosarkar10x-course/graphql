#!/usr/bin/env bash
# Probe 6: the three variants of lab 16.4 through the kube-proxy-like balancer (1 s propagation, no retry).
cd /course/_workbench/2026-11-22-m16-build
(cd resume-app && mvn -q package -DskipTests) && (cd naive-app && mvn -q package -DskipTests) || exit 1
for v in naive-app/target resume-app/target /course/modules/m16-production/contract/ops-app/target; do
  echo "== $v"; bash /course/_workbench/2026-11-22-m16-probe/probe5.sh $v/shelfie-ops.jar 2>&1 | head -1
done
