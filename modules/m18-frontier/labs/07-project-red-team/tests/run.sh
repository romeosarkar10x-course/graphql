#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m18-frontier/contract/mcp.sh
cd "$LAB_DIR" || exit 1
TOKEN=approver-$RANDOM$RANDOM$RANDOM
reset() { curl -s -o /dev/null -X POST $REF/_course/reset-data; }
end() { app_stop; reset; finish; }
reset
app_build target/shelfie-mcp.jar || end
APPROVER_TOKEN=$TOKEN app_start java -jar target/shelfie-mcp.jar || end
mcp_init http://localhost:$PORT/mcp "X-Shelfie-User: u4" || { fail "an MCP session opens"; end; }
names=$(mcp_tools | jq -r '[.[].name] | join(" ")')
[[ $names != *join_club* && $names != *create_review* && $names != *confirm_action* ]] && pass "read-only by default (no write tools without shelfie.mcp.writes=true)" \
  || fail "read-only by default: the write tools are offered without shelfie.mcp.writes=true"
app_stop
APPROVER_TOKEN=$TOKEN app_start java -jar target/shelfie-mcp.jar --shelfie.mcp.writes=true --shelfie.mcp.approval-ttl=3s || end
mine=$(grep -cE '^attack_M[0-9]+\(\)' attacks/mine.sh 2>/dev/null)
bash /course/modules/m18-frontier/contract/redteam/attacks.sh http://localhost:$PORT $TOKEN attacks/mine.sh >/tmp/attacks.txt 2>&1
course=$(grep -cE '^blocked A' /tmp/attacks.txt); own=$(grep -cE '^blocked M' /tmp/attacks.txt)
(( course == 12 )) && pass "the course's 12 attacks are all blocked" || { fail "the course's attacks: $course of 12 blocked"; grep -E '^BYPASS +A' /tmp/attacks.txt | sed 's/^/        /'; }
if (( mine >= 2 && own == mine )); then pass "your $mine attacks (attacks/mine.sh) are blocked by your server"
elif (( mine < 2 )); then fail "attacks/mine.sh needs at least two attacks of your own (attack_M01, attack_M02, ...)"
else fail "your attacks: $own of $mine blocked"; grep -E '^BYPASS +M' /tmp/attacks.txt | sed 's/^/        /'; fi
app_stop; reset
notes_check FINDINGS.md 150 m18/07
finish
