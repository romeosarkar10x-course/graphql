#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR"
for f in nodes.graphqls GlobalId.java Nodes.java; do require_file "$f" || finish; done
rm -rf /tmp/nodes && mkdir -p /tmp/nodes
if javac -cp "$(lib_cp)" -d /tmp/nodes Data.java GlobalId.java Nodes.java "$LAB_HOME/tests/NodeCheck.java" 2>/tmp/nodes.log; then
  if out=$(java -cp "/tmp/nodes:$(lib_cp)" NodeCheck 2>&1); then pass "global ids: encode, decode, node(id:), nodes(ids:) ($(grep -c '^ok' <<<"$out") checks)"
  else fail "global id checks"; grep -vE '^ok' <<<"$out" | head -14 | sed 's/^/        /'; fi
else fail "GlobalId.java and Nodes.java do not compile with the harness" "$(grep error /tmp/nodes.log | head -3)"; fi
finish
