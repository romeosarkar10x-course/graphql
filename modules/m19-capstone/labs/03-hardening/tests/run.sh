#!/usr/bin/env bash
# Capstone stage 3: client, real time, performance and security hardening.
source /course/tools/testlib.sh
source /course/modules/m19-capstone/contract/capstone.sh
source /course/modules/m19-capstone/contract/docs.sh
cap_manifest || finish
cap_api_schema || finish
cap_build || finish
cap_up || finish
echo "  -- security"
probe_auth
probe_trusted
probe_expensive
echo "  -- performance"
probe_n1
echo "  -- real time and clients"
probe_realtime
probe_client
u=$(m .ui); [[ -n $u && -s $LAB_DIR/$u ]] && pass "a UI: $u" || fail "capstone.json \"ui\": one UI of any kind (a page, a TUI, ...)"
echo "  -- evidence in the code"
src=$(find "$LAB_DIR/" -path '*/src/main/*' -name '*.java' -not -path '*/target/*' 2>/dev/null)
grep -lE '@BatchMapping|DataLoader|BatchLoaderRegistry' $src >/dev/null 2>&1 && pass "batch loading in the server (@BatchMapping or DataLoader)" || fail "batch loading: @BatchMapping or a DataLoader"
grep -lE 'sealed interface' $src >/dev/null 2>&1 && pass "sealed types for the abstract types" || fail "sealed Java types for a union or interface"
grep -qE 'spring-boot-starter-(oauth2-resource-server|security)' $(find "$LAB_DIR/" -name pom.xml -not -path '*/target/*') 2>/dev/null \
  && pass "Spring Security in a pom.xml" || fail "Spring Security (a security starter in a pom.xml)"
echo "  -- written evidence"
doc_check attacks 300 "the attack report" 'attack|finding'
doc_check load 200 "the load test" 'result|number'
finish
