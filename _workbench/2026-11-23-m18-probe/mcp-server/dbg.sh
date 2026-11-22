source /course/tools/testlib.sh; source /course/modules/m18-frontier/contract/mcp.sh
cd /course/modules/m18-frontier/labs/02-mcp-server/solution
java -jar target/shelfie-mcp.jar --logging.level.org.springframework.web.client=DEBUG --logging.level.org.springframework.graphql=DEBUG > /tmp/mcp2.log 2>&1 & PID=$!; trap 'kill $PID' EXIT
for _ in $(seq 60); do (exec 3<>/dev/tcp/localhost/4000) 2>/dev/null && break; sleep 0.5; done
mcp_init http://localhost:4000/mcp; mcp_text list_clubs; grep -iE "HTTP|POST|graphql|404" /tmp/mcp2.log | tail -8
