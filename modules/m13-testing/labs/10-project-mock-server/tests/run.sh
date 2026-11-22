#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m13-testing/contract/mock/run-mock.sh
cd "$LAB_DIR" || exit 1
T=$LAB_HOME/tests
app_build target/mock-server.jar || finish
deps=$(unzip -l target/mock-server.jar 2>/dev/null | grep -cE 'graphql/GraphQL.class|tools/jackson/databind/ObjectMapper.class')
[[ $deps == 2 ]] && pass "target/mock-server.jar is standalone (graphql-java and Jackson inside)" \
  || fail "target/mock-server.jar is standalone: java -jar with nothing next to it" "shade or assemble the dependencies into the jar"
# Realistic, deterministic, shape-correct data for four schemas, two of which you have never seen.
p=4100
for s in shelfie shop social forge; do
  if mock_start $p java -jar target/mock-server.jar $T/schemas/$s.graphqls --port $p --seed 13; then
    mock_check $p $T/schemas/$s.graphqls $T/queries/$s "$s.graphqls" --realistic
  fi
  p=$((p + 1))
done
mock_stop
q='{"query":"{ feed(first: 3) { id body author { name } } }"}'
mock_start 4100 java -jar target/mock-server.jar $T/schemas/social.graphqls --port 4100 --seed 1 \
  && mock_start 4101 java -jar target/mock-server.jar $T/schemas/social.graphqls --port 4101 --seed 2
a=$(curl -s -H 'Content-Type: application/json' -d "$q" localhost:4100/graphql); b=$(curl -s -H 'Content-Type: application/json' -d "$q" localhost:4101/graphql)
[[ "$a" == *'"data"'* && "$a" != "$b" ]] && pass "--seed changes the data" || fail "--seed changes the data" "seeds 1 and 2 gave: ${a:0:80}"
mock_stop
# MockSpec: named mocks from files, errors that help, generation of a missing default.
W=$(mktemp -d); trap 'rm -rf $W; mock_stop' EXIT; cp -r $T/mocks $W/mocks
mock_start 4000 java -jar target/mock-server.jar $T/schemas/shelfie.graphqls --port 4000 --mocks $W/mocks || finish
app_hurl $T/mockspec.hurl "MockSpec (mockspec.hurl)"
if [[ -f $W/mocks/NewScreen.json ]] && jq -e '.__default__.data.club.id == "c1"' $W/mocks/NewScreen.json >/dev/null 2>&1; then
  pass "a missing mock file is generated as {\"__default__\": ...} (NewScreen.json)"
else fail "a missing mock file is generated as {\"__default__\": ...}" "no valid $W/mocks/NewScreen.json after M06"; fi
notes_check DESIGN.md 80 m13/10
finish
