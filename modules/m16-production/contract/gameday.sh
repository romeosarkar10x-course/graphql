#!/usr/bin/env bash
# Incident game day (lab 16.8): Shelfie with three hidden faults, its traffic, and the observability stack.
# Run it inside the course image, publishing Grafana to your browser:
#
#   COURSE_PUBLISH=3000 bin/course shell -c 'bash /course/modules/m16-production/contract/gameday.sh [--seed N] [--minutes 6]'
#
# Without --seed it picks one and prints it: write it into answers.json. Grafana: http://localhost:3000 (Explore:
# Prometheus and Tempo). The stack stays up after the traffic stops, until you press Ctrl-C.
source /course/tools/testlib.sh
source /course/modules/m16-production/contract/ops.sh
seed=$(( RANDOM % 1000 )); minutes=6
while (( $# )); do case $1 in --seed) seed=$2; shift 2 ;; --minutes) minutes=$2; shift 2 ;; *) die "unknown option $1" ;; esac; done
JAR=$OPS/ops-app/target/shelfie-ops.jar
[[ -f $JAR ]] || (cd "$OPS/ops-app" && mvn -q package -DskipTests) || die "could not build $JAR"
trap 'kill $APP $TRAFFIC 2>/dev/null; ops_stop; exit 0' EXIT INT TERM
ops_start || exit 1
reviews_start || exit 1
java -Xmx768m -jar "$JAR" --gameday.seed="$seed" >/tmp/gameday-app.log 2>&1 & APP=$!
for _ in $(seq 1 120); do ops_up http://localhost:4000/actuator/health && break; sleep 0.5; done
echo "Game day, seed $seed. Shelfie :4000, Grafana http://localhost:3000 (publish it with COURSE_PUBLISH=3000)."
echo "Traffic runs for $minutes minutes; the stack stays up after that until Ctrl-C."
java -cp "$LIB/*" "$OPS/GameTraffic.java" --seed "$seed" --minutes "$minutes" & TRAFFIC=$!
wait $TRAFFIC
echo "Traffic stopped. Keep investigating; Ctrl-C when you are done."
while true; do sleep 3600; done
