source /course/_workbench/2026-11-23-capstone/probe/lib.sh
cd $R
start_jar inventory 4001 || exit 1
start_jar lending 4002 || exit 1
scripts/router.sh > /tmp/router.log 2>&1 & PIDS+=($!)
for i in $(seq 30); do curl -fs -o /dev/null localhost:4000/health && break; sleep 0.5; done
echo "=== attacks"; scripts/attacks.sh; echo "exit $?"
echo "=== load (nproc $(nproc))"
java scripts/Load.java --seconds 5 --concurrency 8 --token $(java /course/modules/m10-security/contract/Token.java ada --aud toolshed-api --ttl 600) > /dev/null
java scripts/Load.java --seconds 20 --concurrency 8 --token $(java /course/modules/m10-security/contract/Token.java ada --aud toolshed-api --ttl 600)
