#!/usr/bin/env bash
# Module 16's lab tests: source after testlib.sh, ops.sh and m06's app-test.sh, from the lab copy's directory.
#   lab_app_up [extra app args...]   build target/shelfie-ops.jar, start the reviews service and the app on :4000
#   gql_post '<json body>' [headers...]   POST to the app; prints the response body
#   ref_app_jar                      the reference app (contract/ops-app), built once
JAR=target/shelfie-ops.jar
lab_app_up() {
  app_build $JAR || return 1
  reviews_start || return 1
  app_start java -jar $JAR --logging.level.root=warn "$@" || return 1
  for _ in $(seq 1 60); do ops_up http://localhost:4000/actuator/health && return 0; curl -fsS -o /dev/null -X POST localhost:4000/graphql \
    -H 'Content-Type: application/json' -d '{"query":"{ __typename }"}' 2>/dev/null && return 0; sleep 0.5; done
  return 0
}
gql_post() { local body=$1; shift; local h=(); for x in "$@"; do h+=(-H "$x"); done
  curl -sS -m 20 localhost:4000/graphql -H 'Content-Type: application/json' "${h[@]}" -d "$body"; }
ref_app_jar() {
  local jar=$OPS/ops-app/target/shelfie-ops.jar
  [[ -f $jar ]] || (cd "$OPS/ops-app" && mvn -q package -DskipTests >/tmp/ref-build.log 2>&1) || { fail "the reference app builds" "$(grep ERROR /tmp/ref-build.log | head -3)"; return 1; }
  echo "$jar"
}
