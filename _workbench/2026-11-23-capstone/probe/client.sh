source /course/_workbench/2026-11-23-capstone/probe/lib.sh
cd $R && mvn -q -DskipTests package 2>&1 | grep -E "ERROR" | head
start_jar inventory 4001 || exit 1
start_jar lending 4002 || exit 1
scripts/router.sh > /tmp/router.log 2>&1 & PIDS+=($!)
for i in $(seq 30); do curl -fs -o /dev/null localhost:4000/health && break; sleep 0.5; done
C="java -jar client/target/toolshed-client.jar"
$C tools --first 4 --powered; echo "exit $?"
export TOOLSHED_TOKEN=$(java /course/modules/m10-security/contract/Token.java ada --aud toolshed-api)
$C loans; $C borrow t03 --days 2; $C borrow t05; echo "exit $?"
$C ui --port 4005 > /tmp/ui.log 2>&1 & PIDS+=($!)
sleep 2; curl -s localhost:4005/ | head -c 80; echo; curl -s localhost:4005/persisted-documents.json | jq length
curl -s localhost:4005/graphql -H 'content-type: application/json' --data "{\"documentId\":\"$(grep ^Filters tests/ids.env | cut -d= -f2)\"}" | head -c 150; echo
