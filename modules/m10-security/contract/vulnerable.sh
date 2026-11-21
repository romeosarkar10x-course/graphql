#!/usr/bin/env bash
# The vulnerable Shelfie fork (labs 10.3, 10.8, 10.9), inside the course image.
#   vulnerable.sh start [port]   build if the sources changed, start in the background (default :4000)
#   vulnerable.sh stop
#   vulnerable.sh pid
# Heap capped at 512 MB and the JVM exits on OutOfMemoryError, so "crash a node" stays a small crash.
# Sign in as the player account: mutation { login(username: "ben", password: "ben-pw") }
set -euo pipefail
SRC=/course/modules/m10-security/contract/vulnerable-shelfie
JAR=/course/.cache/m10/shelfie-vulnerable.jar
cmd="${1:-start}" port="${2:-4000}"
case "$cmd" in
  start)
    stamp=$(find "$SRC/src" "$SRC/pom.xml" -type f -exec sha256sum {} + | sort | sha256sum | cut -c1-16)
    if [[ ! -f "$JAR" || "$(cat "$JAR.stamp" 2>/dev/null)" != "$stamp" ]]; then
      mkdir -p "$(dirname "$JAR")"; rm -rf /tmp/vulnerable-build; cp -r "$SRC" /tmp/vulnerable-build
      (cd /tmp/vulnerable-build && mvn -q package -DskipTests >/tmp/vulnerable-build.log 2>&1) || { tail -20 /tmp/vulnerable-build.log; exit 1; }
      cp /tmp/vulnerable-build/target/shelfie-vulnerable.jar "$JAR"; echo "$stamp" > "$JAR.stamp"
    fi
    (exec 3<>/dev/tcp/localhost/$port) 2>/dev/null && { echo "port $port is in use" >&2; exit 1; }
    nohup java -Xmx512m -XX:+ExitOnOutOfMemoryError -jar "$JAR" --server.port="$port" >/tmp/vulnerable.log 2>&1 &
    echo $! > /tmp/vulnerable.pid
    for _ in $(seq 1 120); do (exec 3<>/dev/tcp/localhost/$port) 2>/dev/null && { echo "vulnerable Shelfie on :$port (pid $(cat /tmp/vulnerable.pid))"; exit 0; }; sleep 0.5; done
    echo "did not start: /tmp/vulnerable.log" >&2; exit 1 ;;
  stop) [[ -f /tmp/vulnerable.pid ]] && { kill "$(cat /tmp/vulnerable.pid)" 2>/dev/null || true; while kill -0 "$(cat /tmp/vulnerable.pid)" 2>/dev/null; do sleep 0.2; done; rm -f /tmp/vulnerable.pid; } ;;
  pid) cat /tmp/vulnerable.pid ;;
  *) echo "usage: vulnerable.sh start [port] | stop | pid" >&2; exit 2 ;;
esac
