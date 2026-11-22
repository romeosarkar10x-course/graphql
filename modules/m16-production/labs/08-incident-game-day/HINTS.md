# Hints: 16.8 game day

1. Start broad, then narrow: p95 by operation, then the average time per field coordinate. A slow field shows
   in every operation that selects it. In Tempo, filter field spans by duration and see which name keeps
   coming back.
2. A leak is something that only goes up. The game exports a gauge of open subscriptions per client. Every
   client opens and closes subscriptions at the same pace, so they should all look alike. Put that gauge next to
   heap used.
3. "Abusive" is a property of a client, not of an endpoint. Group request rate and cost rate by
   `graphql_client_name`, and compare each client with the others, not with zero. Then find its operation by name.
