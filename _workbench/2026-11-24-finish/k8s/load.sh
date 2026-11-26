# Four clients, a query every 20 ms each, until /tmp/stop exists; one line per request: ok | fail <code>
rm -f /tmp/stop /tmp/res.*
for c in 1 2 3 4; do
  ( while [ ! -f /tmp/stop ]; do
      out=$(curl -s -m 5 -w '\n%{http_code}' shelfie/graphql -H 'Content-Type: application/json' -d '{"query":"{ books(first: 3) { id } }"}')
      code=$(echo "$out" | tail -1)
      if [ "$code" = 200 ] && echo "$out" | grep -q '"books"'; then echo ok; else echo "fail $code"; fi
      sleep 0.02
    done > /tmp/res.$c ) &
done
wait
cat /tmp/res.* | sort | uniq -c
