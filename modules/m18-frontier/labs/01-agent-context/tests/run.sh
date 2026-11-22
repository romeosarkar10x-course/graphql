#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m18-frontier/contract/agent/agent.sh
cd "$LAB_DIR" || exit 1
want=$(cd $AGENT_DIR/operations && ls *.graphql | sort | tr '\n' ' ')
have=$(cd operations 2>/dev/null && ls *.graphql | sort | tr '\n' ' ')
[[ "$have" == "$want" ]] && pass "operations/ holds the ten operations" || fail "operations/ holds the ten operations" "want: $want"
agent ops operations >/tmp/agent-ops.txt 2>&1; rc=$?
if (( rc == 0 )); then pass "all ten have a description that names their variables, parse under the September 2025 grammar and validate against Shelfie"
else fail "$(grep -c '^fail' /tmp/agent-ops.txt) operation(s) not ready for an agent"; grep -v '^ok' /tmp/agent-ops.txt | cut -c1-300 | sed 's/^/        /'; fi
# The operations themselves are the contract's: only descriptions may be added.
same=0
for f in $AGENT_DIR/operations/*.graphql; do
  [[ -f operations/$(basename $f) ]] && diff -q <(agent strip operations/$(basename $f) | tr -s ' \n\t' ' ') <(tr -s ' \n\t' ' ' < $f) >/dev/null && same=$((same + 1))
done
(( same == 10 )) && pass "the operations are unchanged apart from their descriptions" || fail "the operations are unchanged apart from their descriptions" "$((10 - same)) differ from contract/agent/operations"
n=$(ls llm/q[0-9][0-9].graphql 2>/dev/null | wc -l)
if (( n == 10 )); then
  agent score $AGENT_DIR/questions.json llm >llm/score.md 2>&1
  summary=$(tail -1 llm/score.md)
  [[ $summary == queries* ]] && pass "the model's ten queries are scored (llm/score.md): $summary" || fail "agent score did not run" "$(tail -3 llm/score.md)"
else fail "llm/ needs q01.graphql to q10.graphql, the model's queries ($n found)"; fi
notes_check REPORT.md 120 m18/01
finish
