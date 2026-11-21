#!/usr/bin/env bash
# Builds the four reference apps and benchmarks each (run inside bin/course shell from this directory).
set -e
M=/course/modules
build() { (cd "$1" && mvn -q package -DskipTests >/dev/null); }
build $M/m05-graphql-java/labs/09-project-tiny-server/solution
build $M/m06-spring/labs/01-boot-setup/solution
build $M/m06-spring/labs/07-dgs/solution
build $M/m06-spring/labs/08-quarkus/solution
echo "| Stack | Lines | Startup ms | RSS MB | p50 ms | p95 ms | req/s |"
echo "|---|---|---|---|---|---|---|"
for round in 1 2 3; do
java Bench.java "raw graphql-java" $M/m05-graphql-java/labs/09-project-tiny-server/solution/src $M/m05-graphql-java/labs/09-project-tiny-server/solution -- java -jar target/shelfie-server.jar 4000
java Bench.java "Spring for GraphQL" $M/m06-spring/labs/01-boot-setup/solution/src $M/m06-spring/labs/01-boot-setup/solution -- java -jar target/shelfie-boot.jar
java Bench.java "DGS" $M/m06-spring/labs/07-dgs/solution/src $M/m06-spring/labs/07-dgs/solution -- java -jar target/shelfie-dgs.jar
java Bench.java "Quarkus" $M/m06-spring/labs/08-quarkus/solution/src $M/m06-spring/labs/08-quarkus/solution -- java -jar target/quarkus-app/quarkus-run.jar
done
