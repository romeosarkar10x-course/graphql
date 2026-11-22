# Mini-project 18.7: Agent safety lab

Lecture: [18.3 Agent safety](../../lectures/18.3-agent-safety.md). Grading: auto + rubric. Needs the reference
server.

The starter is a Shelfie MCP server that looks like lab 18.3's: the same tools, the same guard, approvals, an
audit log. It was reviewed in a hurry, and **seven weaknesses** got through. Red-team it: find them, prove each
with an attack, fix it, and keep the attack as a regression test.

Play it in this order, because that is where the points are:

1. **Attack blind.** Read the code, run the server, and attack it with the course's MCP client
   (`contract/mcp.sh`) or a real assistant. Do not run `bin/course test` yet. Every weakness you find this way,
   with the time you found it, goes in `FINDINGS.md`.
2. **Write your attacks** in `attacks/mine.sh` (at least two; the format is in the file): weaknesses you found, or
   attacks the course might not have thought of. They must be blocked by your fixed server.
3. **Then run the course's attacks** (`bin/course test m18/07`): 12 of them, in
   [contract/redteam/attacks.sh](../../contract/redteam/attacks.sh). Fix what they still find.

```bash
bin/course start m18/07
cd workspace/m18-frontier/07-project-red-team
bin/course shell     # then, inside the image:
  mvn -q package && (APPROVER_TOKEN=s3cret java -jar target/shelfie-mcp.jar --shelfie.mcp.writes=true --shelfie.mcp.approval-ttl=3s &)
  source /course/tools/testlib.sh; source /course/modules/m18-frontier/contract/mcp.sh
  mcp_init http://localhost:4000/mcp "X-Shelfie-User: u4" && mcp_tools | jq '.[].name'     # attack by hand
  bash /course/modules/m18-frontier/contract/redteam/attacks.sh http://localhost:4000 s3cret attacks/mine.sh   # step 3
bin/course test m18/07
```

**Scoring** (`rubric.md`): 2 points per seeded weakness found blind, 1 per weakness the course's attacks had to
show you (fixed either way), 2 per attack of your own that the course's list does not have.

## Group variant

Teams of two or three. Each team takes the starter, fixes what it can in 30 minutes, writes its attacks, then
publishes its server (`COURSE_PUBLISH=4000`, the token handed to the referee only). Teams run their
`attacks/mine.sh` against every other team's server (`attacks.sh http://<their-ip>:4000 <token from the
referee> attacks/mine.sh`); a `BYPASS` line is a point for the attacker, shown to the defender, who may fix and
redeploy once. Final round: everyone runs the course's 12. Highest attack plus defence total wins.

## Done when

`bin/course test m18/07` passes (read-only by default, the 12 attacks and yours blocked) and `FINDINGS.md` passes
the rubric (`bin/course grade m18/07`).
