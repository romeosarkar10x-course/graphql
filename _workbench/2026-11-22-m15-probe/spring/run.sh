#!/usr/bin/env bash
# usage: run.sh <profile> <port> <schema-dir>
exec java -XX:TieredStopAtLevel=1 -Xmx160m -jar target/fed-probe.jar --spring.profiles.active=$1 --server.port=$2 --spring.graphql.schema.locations=classpath:graphql/$3/
