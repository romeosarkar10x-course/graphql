# Source after testlib.sh. kafka_tool <args>: runs KafkaTool.java with kafka-clients from the Maven cache (the
# lab's own build downloaded it; Spring Boot 4.2's BOM pins 4.4.0).
kafka_cp() {
  local k s
  k=$(find /course/.cache/m2/org/apache/kafka/kafka-clients -name 'kafka-clients-4.4.0.jar' | head -1)
  s=$(find /course/.cache/m2/org/slf4j/slf4j-api -name 'slf4j-api-2.*.jar' | sort | tail -1)
  echo "$k:$s"
}
kafka_tool() { java -cp "$(kafka_cp)" /course/modules/m14-patterns/contract/KafkaTool.java "$@" 2>/dev/null; }
