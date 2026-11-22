# Module 18 agent helpers (contract/agent/Agent.java) inside the course image. Source after tools/testlib.sh, or run
#   bin/course shell -c 'source /course/tools/testlib.sh; source /course/modules/m18-frontier/contract/agent/agent.sh; agent score ...'
#   agent ops|strip|cost|score ...   see Agent.java; Module 17's engine (the September 2025 parser) is built once
AGENT_DIR=/course/modules/m18-frontier/contract/agent
MINIGQL=/course/modules/m17-under-the-hood/contract/engine/target/minigql.jar
agent() {
  [[ -f $MINIGQL ]] || (cd "$(dirname "$(dirname "$MINIGQL")")" && mvn -q package -DskipTests >/tmp/minigql-build.log 2>&1) \
    || { echo "course: Module 17's engine did not build (/tmp/minigql-build.log)" >&2; return 2; }
  java -cp "$(lib_cp):$MINIGQL" $AGENT_DIR/Agent.java "$@"
}
