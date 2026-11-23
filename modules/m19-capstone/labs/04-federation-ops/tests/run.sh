#!/usr/bin/env bash
# Capstone stage 4: federation, observability and CI.
source /course/tools/testlib.sh
source /course/modules/m19-capstone/contract/capstone.sh
source /course/modules/m19-capstone/contract/docs.sh
cap_manifest || finish
n=$(m '.subgraphs | length'); (( n >= 2 )) && pass "$n subgraphs" || fail "at least 2 subgraphs behind a router ($n now)"
cap_api_schema || finish
cap_build || finish
cap_ops_start || finish
cap_up || finish
echo "  -- the graph"
probe_served_schema
cap_hurl
echo "  -- observability"
probe_trace
sleep 3
probe_dashboard
doc_check runbook 250 "the runbook"
f=$(m .docs.runbook)
a=$(grep -ciE '^#+ .*(alert|when|symptom|page)' "$LAB_DIR/$f" 2>/dev/null); q=$(grep -cE '(rate|sum|histogram_quantile|increase|max|avg)\(' "$LAB_DIR/$f" 2>/dev/null)
(( a >= 3 && q >= 3 )) && pass "$f: $a alerts or symptoms, $q PromQL conditions" || fail "$f: at least 3 alert sections, each with a PromQL condition" "found ${a:-0} sections, ${q:-0} queries"
echo "  -- CI gates (read, not run)"
ci=$(m .ci)
if [[ -n $ci && -s $LAB_DIR/$ci ]]; then
  ci_gate() { grep -qiE "$2" "$LAB_DIR/$ci" && pass "CI: $1" || fail "CI: a step for $1"; }
  ci_gate "build and tests (mvn verify or test)" 'mvn[^#]*(verify|test)'
  ci_gate "mutation testing (PIT)" 'pitest|-Ppit|mutationCoverage'
  ci_gate "the composed schema and trusted documents are up to date" 'compose|supergraph|persist|persisted'
  tests=$(find "$LAB_DIR/" -path '*/src/test/*' -name '*.java' -not -path '*/target/*' 2>/dev/null)
  grep -lq 'GraphQlTester' $tests 2>/dev/null && pass "tests use GraphQlTester" || fail "a test with GraphQlTester (or HttpGraphQlTester)"
  grep -lq -i 'testcontainers' $tests 2>/dev/null && pass "tests use Testcontainers" || fail "a Testcontainers test"
  grep -lqiE 'statement|n\+1|querycount' $tests 2>/dev/null && pass "a test counts statements (the N+1 detector)" || fail "an N+1 test that counts statements"
  grep -lqiE 'SchemaDiff|schema.?(compat|diff|check)' $tests "$LAB_DIR/$ci" 2>/dev/null && pass "a schema-diff gate" || fail "a schema-diff gate (a test or a CI step)"
else fail "capstone.json \"ci\": the CI workflow file"; fi
finish
