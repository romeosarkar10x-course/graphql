S=/course/modules/m09-performance/labs/07-measuring/solution
java -cp "$(source /course/tools/testlib.sh; lib_cp)" /course/modules/m09-performance/contract/ClubData.java m09_07 >/dev/null
cd $S/app && mvn -q package -DskipTests 2>&1 | grep ERROR | head -3
java -jar target/shelfie-perf.jar > /tmp/app.log 2>&1 & pid=$!
for i in $(seq 80); do (exec 3<>/dev/tcp/localhost/4000) 2>/dev/null && break; sleep 0.5; done
cd $S/load && time mvn -q gatling:test -Dgatling.simulationClass=dev.shelfie.load.DashboardSimulation -Drate=20 -Dseconds=10 2>&1 | tail -25
ls target/gatling/*/ target/gatling/*/js 2>/dev/null | head -20
kill $pid
