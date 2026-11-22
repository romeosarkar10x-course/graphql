#!/usr/bin/env bash
# Module 16's helper library: source it after testlib.sh. Everything runs inside the course container, on
# localhost, and stops with the test (ops_stop runs on exit when you use ops_trap).
#
#   ops_start [tempo] [prometheus] [grafana]   start those (default: all three) and wait until ready
#       Tempo       OTLP :4317 gRPC, :4318 HTTP; query API and TraceQL on :3200
#       Prometheus  :9090, scrapes localhost:4000-4003/actuator/prometheus every second
#       Grafana     :3000, anonymous admin, data sources "prometheus" and "tempo" (uids); dashboards from
#                   $OPS_DASHBOARDS (default contract/ops/grafana/dashboards)
#   ops_stop                                  stop them all (data under /tmp/ops is deleted)
#   reviews_start [port] / reviews_stop       the reviews service (contract/ReviewsService.java), default :4100
#   reviews_fault ok|slow|error [ms]          switch the reviews service's behaviour
#   promq '<PromQL>'                           instant query; prints the "result" array as JSON
#   promq_wait '<PromQL>' [seconds]            until the query returns at least one series (default 20 s)
#   traceql '<TraceQL>' [limit] [spss]         Tempo search; prints the "traces" array as JSON
#   tempo_trace <traceID>                      the whole trace as OTLP JSON
#
# By hand, from bin/course shell (publish ports with COURSE_PUBLISH, one port per shell):
#   source /course/tools/testlib.sh; source /course/modules/m16-production/contract/ops.sh; ops_start
OPS=/course/modules/m16-production/contract
OPS_BIN=/course/.cache/ops/bin
OPS_TMP=/tmp/ops
OPS_PIDS=()
REVIEWS_PID=
export OPS_DASHBOARDS="${OPS_DASHBOARDS:-$OPS/ops/grafana/dashboards}"

ops_up() { curl -fsS -o /dev/null -m 2 "$1" 2>/dev/null; }
ops_wait() { local url=$1 name=$2 log=$3; for _ in $(seq 1 90); do ops_up "$url" && return 0; sleep 0.5; done
  fail "$name did not start" "$(tail -3 "$log" 2>/dev/null)"; return 1; }

ops_start() {
  local want=("$@"); (( ${#want[@]} )) || want=(tempo prometheus grafana)
  mkdir -p "$OPS_TMP"
  for w in "${want[@]}"; do
    case $w in
      tempo)
        ops_up http://localhost:3200/ready && continue
        rm -rf "$OPS_TMP/tempo"; mkdir -p "$OPS_TMP/tempo"
        "$OPS_BIN/tempo" -config.file="$OPS/ops/tempo.yaml" >"$OPS_TMP/tempo.log" 2>&1 & OPS_PIDS+=($!)
        ops_wait http://localhost:3200/ready Tempo "$OPS_TMP/tempo.log" || return 1 ;;
      prometheus)
        ops_up http://localhost:9090/-/ready && continue
        rm -rf "$OPS_TMP/prometheus"
        "$OPS_BIN/prometheus" --config.file="$OPS/ops/prometheus.yaml" --storage.tsdb.path="$OPS_TMP/prometheus" \
          --web.listen-address=0.0.0.0:9090 --web.enable-lifecycle >"$OPS_TMP/prometheus.log" 2>&1 & OPS_PIDS+=($!)
        ops_wait http://localhost:9090/-/ready Prometheus "$OPS_TMP/prometheus.log" || return 1 ;;
      grafana)
        ops_up http://localhost:3000/api/health && continue
        rm -rf "$OPS_TMP/grafana"; mkdir -p "$OPS_TMP/grafana"
        # Anonymous admin: a local, throwaway Grafana. Usage reporting and update checks off: nothing leaves the machine.
        GF_PATHS_DATA="$OPS_TMP/grafana" GF_PATHS_LOGS="$OPS_TMP/grafana" GF_PATHS_PROVISIONING="$OPS/ops/grafana" \
        GF_AUTH_ANONYMOUS_ENABLED=true GF_AUTH_ANONYMOUS_ORG_ROLE=Admin GF_AUTH_DISABLE_LOGIN_FORM=true \
        GF_ANALYTICS_REPORTING_ENABLED=false GF_ANALYTICS_CHECK_FOR_UPDATES=false GF_ANALYTICS_CHECK_FOR_PLUGIN_UPDATES=false \
        GF_NEWS_NEWS_FEED_ENABLED=false GF_LOG_LEVEL=warn \
          /course/.cache/ops/grafana/bin/grafana server --homepath /course/.cache/ops/grafana >"$OPS_TMP/grafana.log" 2>&1 & OPS_PIDS+=($!)
        ops_wait http://localhost:3000/api/health Grafana "$OPS_TMP/grafana.log" || return 1 ;;
    esac
  done
}
ops_stop() {
  local p; for p in "${OPS_PIDS[@]}"; do kill "$p" 2>/dev/null; done
  for p in "${OPS_PIDS[@]}"; do wait "$p" 2>/dev/null; done
  OPS_PIDS=(); reviews_stop; return 0
}

reviews_start() {
  local port=${1:-4100}
  java -cp "$LIB/*" "$OPS/ReviewsService.java" "$port" >/tmp/reviews.log 2>&1 & REVIEWS_PID=$!
  for _ in $(seq 1 60); do ops_up "http://localhost:$port/_stats" && return 0; sleep 0.5; done
  fail "the reviews service did not start" "$(tail -3 /tmp/reviews.log)"; return 1
}
reviews_stop() { [[ -n "$REVIEWS_PID" ]] && { kill "$REVIEWS_PID" 2>/dev/null; wait "$REVIEWS_PID" 2>/dev/null; REVIEWS_PID=; }; return 0; }
reviews_fault() { curl -fsS -o /dev/null -X POST "http://localhost:4100/_fault?mode=$1&ms=${2:-2000}"; }

promq() { curl -fsS -G http://localhost:9090/api/v1/query --data-urlencode "query=$1" | jq -c '.data.result'; }
promq_wait() { local r; for _ in $(seq 1 $(( ${2:-20} * 2 ))); do r=$(promq "$1"); [[ "$r" != "[]" && -n "$r" ]] && { echo "$r"; return 0; }; sleep 0.5; done; echo "[]"; return 1; }
traceql() { curl -fsS -G http://localhost:3200/api/search --data-urlencode "q=$1" --data-urlencode "limit=${2:-20}" \
  --data-urlencode "spss=${3:-50}" --data-urlencode "start=$(( $(date +%s) - 3600 ))" --data-urlencode "end=$(( $(date +%s) + 60 ))" | jq -c '.traces // []'; }
tempo_trace() { curl -fsS -H 'Accept: application/json' "http://localhost:3200/api/v2/traces/$1"; }
# The OpenTelemetry Java agent 2.33.1 (Maven Central), fetched once into .cache/ops; prints its path.
#   java -javaagent:$(otel_agent) -Dotel.service.name=shelfie -Dotel.metrics.exporter=none -Dotel.logs.exporter=none -jar app.jar
otel_agent() {
  local jar=/course/.cache/ops/opentelemetry-javaagent-2.33.1.jar
  [[ -f $jar ]] || mvn -q dependency:copy -Dartifact=io.opentelemetry.javaagent:opentelemetry-javaagent:2.33.1 \
    -DoutputDirectory=/course/.cache/ops >/tmp/otel-agent.log 2>&1 || { echo "could not fetch the agent: see /tmp/otel-agent.log" >&2; return 1; }
  echo "$jar"
}
