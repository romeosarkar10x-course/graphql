# Median and p95 of 100 dashboards: naive (9.1), batched (9.3), one statement (9.4). Warm-up 20.
M=/course/modules/m09-performance/labs
jq -n --rawfile q /course/modules/m09-performance/contract/dashboard.graphql '{query: $q}' > /tmp/d.json
for lab in 01-seeing-n-plus-one:m09_01 03-batch-loading:m09_03 04-lookahead-sql:m09_04; do
  dir=${lab%%:*}; schema=${lab##*:}
  java -cp "$(source /course/tools/testlib.sh; lib_cp)" /course/modules/m09-performance/contract/ClubData.java $schema >/dev/null
  (cd $M/$dir/solution && java -jar target/shelfie-perf.jar > /tmp/app-$dir.log 2>&1) & 
  for i in $(seq 80); do (exec 3<>/dev/tcp/localhost/4000) 2>/dev/null && break; sleep 0.5; done
  for i in $(seq 20); do curl -s -o /dev/null -H 'Content-Type: application/json' -d @/tmp/d.json localhost:4000/graphql; done
  for i in $(seq 100); do curl -s -o /dev/null -w '%{time_total}\n' -H 'Content-Type: application/json' -d @/tmp/d.json localhost:4000/graphql; done | sort -n > /tmp/t.txt
  printf '%-22s median %6.1f ms  p95 %6.1f ms\n' $dir $(awk 'NR==50{print $1*1000}' /tmp/t.txt) $(awk 'NR==95{print $1*1000}' /tmp/t.txt)
  pkill -f shelfie-perf.jar; sleep 1
done
grep -m1 'Executing query' /tmp/app-04-lookahead-sql.log | sed 's/.*Executing query *: //' > /course/_workbench/2026-11-21-m09-probe/lookahead-dashboard.sql
