cd /course/modules/m09-performance/labs/05-caching-layers/solution
java -cp "$(source /course/tools/testlib.sh; lib_cp)" /course/modules/m09-performance/contract/ClubData.java m09_05 >/dev/null
exec 3<>/dev/tcp/redis/6379; printf 'FLUSHALL\r\n' >&3; read -r l <&3; exec 3>&-
java -jar target/shelfie-perf.jar > /tmp/app.log 2>&1 & pid=$!
for i in $(seq 60); do (exec 3<>/dev/tcp/localhost/4000) 2>/dev/null && break; sleep 0.5; done
hurl --very-verbose --variable url=http://localhost:4000/graphql ../tests/scenario-1.hurl 2>&1 | grep -E '^\{|"data"|errors' | head -5
hurl --variable url=http://localhost:4000/graphql ../tests/scenario-2.hurl; echo
grep -E "update book|ERROR|Exception" /tmp/app.log | head -8
kill $pid
