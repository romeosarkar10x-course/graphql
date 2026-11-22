#!/usr/bin/env bash
# attacks.sh against a lab directory's jar: bash redteam-run.sh <lab dir>
source /course/tools/testlib.sh; source /course/modules/m06-spring/contract/app-test.sh
cd "$1" && mvn -q package -DskipTests >/tmp/rt-build.log 2>&1 || { tail /tmp/rt-build.log; exit 1; }
APPROVER_TOKEN=tok-12345678 app_start java -jar target/shelfie-mcp.jar --shelfie.mcp.writes=true --shelfie.mcp.approval-ttl=3s || exit 1
bash /course/modules/m18-frontier/contract/redteam/attacks.sh http://localhost:$PORT tok-12345678
