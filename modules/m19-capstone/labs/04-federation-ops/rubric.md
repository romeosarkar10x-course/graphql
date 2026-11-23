# Rubric, stage 4 (15 points)

| Criterion | Points | Full marks |
| --- | --- | --- |
| Subgraph boundaries | 4 | Each subgraph owns its data; the entity between them is justified; what one subgraph's outage does is designed, not discovered |
| Tracing | 3 | One trace across the router and the subgraphs, with spans that say what ran (fields, loaders, SQL), not only HTTP |
| Dashboard | 3 | Panels an operator of this product needs (an N+1 alarm, latency per subgraph, a product metric), each with a description |
| Runbook | 2 | Alerts with conditions and actions a stranger could follow |
| CI | 3 | Every gate present and meaningful: the schema-diff gate compares with a released schema, PIT has a threshold, stale artifacts fail the build |

Pass: 10/15.
