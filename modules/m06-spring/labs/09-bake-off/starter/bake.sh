#!/usr/bin/env bash
# Builds your four apps and benchmarks each (run inside bin/course shell from this directory).
set -e
W=/course/workspace   # your own apps from m05/09, m06/01, m06/07 and m06/08
build() { (cd "$1" && mvn -q package -DskipTests >/dev/null); }
build $W/m05-graphql-java/09-project-tiny-server
build $W/m06-spring/01-boot-setup
build $W/m06-spring/07-dgs
build $W/m06-spring/08-quarkus
echo "| Stack | Lines | Startup ms | RSS MB | p50 ms | p95 ms | req/s |"
echo "|---|---|---|---|---|---|---|"
for round in 1 2 3; do
java Bench.java "raw graphql-java" $W/m05-graphql-java/09-project-tiny-server/src $W/m05-graphql-java/09-project-tiny-server -- java -jar target/shelfie-server.jar 4000
java Bench.java "Spring for GraphQL" $W/m06-spring/01-boot-setup/src $W/m06-spring/01-boot-setup -- java -jar target/shelfie-boot.jar
java Bench.java "DGS" $W/m06-spring/07-dgs/src $W/m06-spring/07-dgs -- java -jar target/shelfie-dgs.jar
java Bench.java "Quarkus" $W/m06-spring/08-quarkus/src $W/m06-spring/08-quarkus -- java -jar target/quarkus-app/quarkus-run.jar
done
