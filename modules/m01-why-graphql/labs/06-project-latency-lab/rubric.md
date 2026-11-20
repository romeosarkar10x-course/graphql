# Rubric, mini-project 1.6 page (10 points)

| Criterion | Points | Full marks |
|---|---|---|
| Correct model | 3 | Bars match `LatencyModel`'s formula with the given call counts and bytes; spot-check: 700 ms RTT, 10 ms server, 1 Mbit/s, 6 parallel gives REST ≈ 2,887 ms, GraphQL ≈ 717 ms |
| Usable | 3 | Four sliders with visible values and units; bars and numbers update live; works at phone width |
| Insight | 4 | Answers both questions with numbers: the crossover point, and a REST-wins setting (e.g. near-zero RTT with high bandwidth and large parallelism, where GraphQL's single bigger call plus server cost loses) with the reason |

Pass: 7/10.
