#!/usr/bin/env bash
# Probe 11: the knee of the reference app on the club page (cost 38), then the HPA simulation.
source /course/tools/testlib.sh; source /course/modules/m16-production/contract/ops.sh
trap 'kill $APP 2>/dev/null; ops_stop' EXIT
reviews_start || exit 1
taskset -c 0 java -XX:ActiveProcessorCount=1 -jar ${1:-$OPS/ops-app/target/shelfie-ops.jar} --management.tracing.enabled=false --logging.level.root=warn > /tmp/app.log 2>&1 & APP=$!
for i in $(seq 60); do curl -fs -o /dev/null localhost:4000/actuator/health && break; sleep 0.5; done
java -cp "$LIB/*" $OPS/Peak.java --seconds 5 --rate 200 > /dev/null   # warm up
nproc
bash $OPS/ramp.sh 5 ${RATES:-100 200 400 600 800 1000}
