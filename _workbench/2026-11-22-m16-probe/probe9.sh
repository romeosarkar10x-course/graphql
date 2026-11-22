#!/usr/bin/env bash
# Probe 9: game day seed 3 for 90 s; can the three faults be seen from metrics alone?
source /course/tools/testlib.sh; source /course/modules/m16-production/contract/ops.sh
java -cp "$LIB/*" -e 2>/dev/null
cat > /tmp/Answers.java <<'J'
public class Answers { public static void main(String[] a) throws Exception {
  var m = Class.forName("GameTraffic").getDeclaredMethod("answers", long.class); m.setAccessible(true);
  System.out.println(m.invoke(null, Long.parseLong(a[0]))); } }
J
timeout 150 bash $OPS/gameday.sh --seed 3 --minutes 1 > /tmp/gd.log 2>&1 &
sleep 75
echo "== slowest coordinates (avg ms)"; promq 'topk(4, sum by (graphql_field_coordinate) (rate(graphql_datafetcher_seconds_sum[1m])) / sum by (graphql_field_coordinate) (rate(graphql_datafetcher_seconds_count[1m])) * 1000)' | jq -c '.[] | [.metric.graphql_field_coordinate, (.value[1]|tonumber|floor)]'
echo "== subscriptions open"; promq 'shelfie_subscriptions_open' | jq -c '.[] | [.metric.graphql_client_name, .value[1]]'
echo "== heap MB"; promq 'sum(jvm_memory_used_bytes{area="heap"}) / 1e6' | jq -c '.[].value[1]'
echo "== cost per second by client"; promq 'sum by (graphql_client_name) (rate(graphql_cost_sum[1m]))' | jq -c '.[] | [.metric.graphql_client_name, (.value[1]|tonumber|floor)]'
echo "== requests per second by client"; promq 'sum by (graphql_client_name) (rate(graphql_request_seconds_count[1m]))' | jq -c '.[] | [.metric.graphql_client_name, (.value[1]|tonumber*10|floor/10)]'
echo "== answer key"; grep -o 'answers(long seed)' $OPS/GameTraffic.java >/dev/null && echo "(see GameTraffic.answers(3))"
cat /tmp/gd.log | head -3
wait
