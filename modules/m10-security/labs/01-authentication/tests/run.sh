#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
KC=http://keycloak:8180/realms/shelfie/protocol/openid-connect/token
kc() { curl -s -d grant_type=password -d client_id=shelfie-cli -d username="$1" -d password="$1-pw" "$KC" | jq -r .access_token; }
T="java /course/modules/m10-security/contract/Token.java"
kc_ada=$(kc ada); kc_max=$(kc max)
[[ "$kc_ada" == ey* ]] && pass "Keycloak issues tokens for the shelfie realm" || { fail "Keycloak issues tokens" "bin/course ref up, then retry"; finish; }
app_build target/shelfie-secure.jar || finish
app_start java -jar target/shelfie-secure.jar || finish
app_hurl "$LAB_HOME/tests/auth.hurl" "authentication (auth.hurl)" \
  --variable "kc_ada=$kc_ada" --variable "kc_max=$kc_max" \
  --variable "expired=$($T ada --expired)" --variable "forged=$($T ada --forged)" \
  --variable "none=$($T root --none)" --variable "wrong_aud=$($T ada --aud other-api)" \
  --variable "wrong_iss=$($T ada --iss http://evil.test/realms/shelfie)"
app_stop
grep -rq '@AuthenticationPrincipal' src/main/java && pass "viewer reads the principal with @AuthenticationPrincipal" \
  || fail "viewer reads the principal with @AuthenticationPrincipal" "lecture 10.1: let Spring hand the resolver the verified token"
notes_check notes.md 60 m10/01
finish
