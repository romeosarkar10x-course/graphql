# Rubric, lab 1.3 notes.md (10 points)

| Criterion | Points | Full marks |
|---|---|---|
| Comparison table | 3 | Both rows complete and plausible (GraphQL ≈ 1 round trip at 700 ms; REST ≈ waves × 700 ms) |
| Honest reading | 2 | Notices or explains if GraphQL is not faster without latency, rather than claiming a universal win |
| Where the work went | 3 | Joins moved from client over the network to server in-process; names the new server-side burden (N+1, expensive queries) |
| What was lost | 2 | At least one real loss: URL-based HTTP/CDN caching, per-endpoint status codes or metrics, simplicity |

Pass: 7/10.
