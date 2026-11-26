rm -f /tmp/stop
for c in 1 2 3 4; do ( while [ ! -f /tmp/stop ]; do curl -s -o /dev/null shelfie/graphql -H 'Content-Type: application/json' -d '{"query":"{ books(first: 50) { id title reviews { rating author { name } } } }"}'; sleep 0.05; done ) & done; wait
