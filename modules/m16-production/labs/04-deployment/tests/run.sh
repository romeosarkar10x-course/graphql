#!/usr/bin/env bash
# Lab 16.4: health probes, then a rolling deploy of two replicas behind a readiness-aware balancer while 200
# subscribers listen and 4 clients query: nothing may be dropped, duplicated or fail. Then Jib and the manifest.
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m16-production/contract/ops.sh
source /course/modules/m16-production/contract/rollout.sh
cd "$LAB_DIR" || exit 1
trap 'rollout_cleanup; ops_stop' EXIT
JAR=$PWD/target/shelfie-ops.jar
app_build target/shelfie-ops.jar || finish
reviews_start || finish
if replica_start "$JAR" 4001; then
  l=$(curl -s -o /dev/null -w '%{http_code}' localhost:4001/actuator/health/liveness)
  [[ $l == 200 ]] && pass "liveness and readiness probes answer 200" || fail "/actuator/health/liveness answers $l"
  replica_stop 4001
else fail "/actuator/health/readiness answers 200 once the app is up" "$(curl -s localhost:4001/actuator/health/readiness | head -c 120)"; finish; fi
rm -f /tmp/rollout.log
R=$(rollout_run "$JAR" 200 25 2>/dev/null | tail -1)
note "rolling deploy: $R"
if ! jq -e .clients <<<"$R" >/dev/null 2>&1; then fail "the rolling deploy ran" "$R"; finish; fi
[[ $(jq .dropped <<<"$R") == 0 ]] && pass "no subscription message dropped ($(jq .written <<<"$R") messages x 200 subscribers, $(jq .reconnects <<<"$R") reconnects)" \
  || fail "messages dropped during the deploy: $(jq .dropped <<<"$R")" "a reconnecting client passes after: <last seq>; serve what it missed, then the live feed"
[[ $(jq .duplicates <<<"$R") == 0 ]] && pass "no message delivered twice" || fail "duplicates: $(jq .duplicates <<<"$R")"
[[ $(jq .unsubscribedAtEnd <<<"$R") == 0 ]] && pass "every subscriber is subscribed again at the end" || fail "subscribers left without a subscription: $(jq .unsubscribedAtEnd <<<"$R")"
[[ $(jq .failedQueries <<<"$R") == 0 ]] && pass "no query failed during the deploy ($(jq .queries <<<"$R") queries)" \
  || fail "queries failed during the deploy: $(jq .failedQueries <<<"$R") of $(jq .queries <<<"$R")" "readiness first, then wait longer than the balancer needs to notice, then stop"
pom=$(tr -d '\n' < pom.xml)
grep -q '<artifactId>jib-maven-plugin</artifactId>' <<<"$pom" && grep -qE '<from>\s*<image>[^<]+</image>' <<<"$pom" \
  && grep -qE '<port>4000</port>' <<<"$pom" && grep -qE '<user>[1-9][0-9]*(:[0-9]+)?</user>' <<<"$pom" \
  && pass "Jib builds the image (base image, port 4000, a non-root user)" || fail "pom.xml: jib-maven-plugin with from.image, container.ports 4000 and a non-root container.user"
K=$(ls k8s/*.yaml k8s/*.yml 2>/dev/null | head -1)
if [[ -n $K ]] && J=$(java -cp "$LIB/*" "$OPS/Yaml.java" "$K" 2>/tmp/yaml.err); then
  d=$(jq -c '[.[] | select(.kind == "Deployment")][0]' <<<"$J"); c=$(jq -c '.spec.template.spec.containers[0]' <<<"$d")
  [[ $(jq -r '.readinessProbe.httpGet.path' <<<"$c") == /actuator/health/readiness && $(jq -r '.livenessProbe.httpGet.path' <<<"$c") == /actuator/health/liveness ]] \
    && pass "manifest: readiness and liveness probes on the Actuator groups" || fail "$K: readinessProbe and livenessProbe httpGet paths"
  g=$(jq '.spec.template.spec.terminationGracePeriodSeconds // 30' <<<"$d")
  (( g >= 25 )) && [[ $(jq '.spec.strategy.rollingUpdate.maxUnavailable // "25%"' <<<"$d") == 0 && $(jq '.spec.replicas' <<<"$d") -ge 2 ]] \
    && pass "manifest: rolls with maxUnavailable 0 and gives the app time to stop (${g} s)" \
    || fail "$K: replicas >= 2, strategy.rollingUpdate.maxUnavailable 0, terminationGracePeriodSeconds >= 25 (drain + in-flight requests)"
else fail "a Kubernetes manifest k8s/*.yaml that parses" "$(head -2 /tmp/yaml.err 2>/dev/null)"; fi
require_file DEPLOY.md "the deploy notes (rubric)" && notes_check DEPLOY.md 150 m16/04
finish
