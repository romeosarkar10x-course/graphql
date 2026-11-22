# Game 16.8: Incident game day

Lecture: [16.5 Reliability](../../lectures/16.5-reliability.md) (and 16.1). Grading: auto (the three answers) +
rubric (the report). Needs `bin/course ops`.

Shelfie is in trouble. `contract/gameday.sh` starts:

- Shelfie with **three hidden faults**, chosen by a seed;
- its usual traffic from four clients (`web`, `ios`, `android`, `partner-bulk`);
- Prometheus, Tempo and Grafana.

Find all three faults using **traces and metrics only**. No reading the code, no logs.

1. **A slow field.** One field got slow. Which one (`Type.field`)?
2. **A memory leak.** One client's subscriptions are never released. Which client?
3. **An abusive client.** One client is hammering the API with expensive queries. Which one?

Sources you can use:

- Prometheus: the metrics from lab 16.1, plus `graphql_cost_*` and the game's `shelfie_subscriptions_open`;
- JVM metrics;
- Tempo (TraceQL; search lags 20 to 30 s).

```bash
bin/course start m16/08
COURSE_PUBLISH=3000 bin/course shell -c 'bash /course/modules/m16-production/contract/gameday.sh --minutes 6'
# note the seed it prints; open http://localhost:3000 (Explore); Ctrl-C when done
```

Write `answers.json` (the seed and your three answers) and `INCIDENTS.md`. For each incident, the report has:

- the symptom;
- the query that proved it and what it returned;
- the cause;
- the mitigation now and the fix later;
- the alert that would have caught it sooner.

```bash
cd workspace/m16-production/08-incident-game-day
bin/course test m16/08
```

## Solo variant

Run without `--seed`: the script picks a seed at random and you do not know the answers. Time yourself. A good
first run takes under 20 minutes for all three.

## Group variant

One person is the **game master**:

1. Pick a seed in secret and run `COURSE_PUBLISH=3000 bin/course shell -c 'bash .../gameday.sh --seed N'`.
2. Players open Grafana at `http://<game master's machine>:3000`.
3. The first team to name all three faults with evidence wins.

Teams split up: one person on traces, one on metrics, one keeping the timeline. Afterwards, write one
`INCIDENTS.md` per team. The game master reveals the seed, and everyone puts it in `answers.json`.

## Done when

`bin/course test m16/08` passes and `INCIDENTS.md` passes the rubric (`bin/course grade m16/08`).
