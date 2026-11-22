# Hints: m15/06

1. Both routers group header rules under `headers.all.request`, then a list of rules; a rule propagates one named
   header. Anything not named is not forwarded, so the cookie rule is free once the other two work.
2. Introspection lives under `supergraph` in both. Plans are exposed only when a plugin (Apollo:
   `plugins.experimental.expose_query_plan`) or a planner option (Hive: `query_planner.allow_expose`) says so;
   leave it out.
3. Timeouts are traffic shaping in both, per subgraph or for `all` subgraphs, and the keys are named differently
   (`timeout` against `request_timeout`). The field survives the timeout only because `averageRating` is nullable:
   check what the router returns for the rest of the book.
