# Playing with friends

Every game and group activity has a solo and a group variant. For the group variants:

1. Each player clones the repo and runs `bin/course doctor`. Everyone has their own `workspace/`.
2. Pick a host for the round. The host shares the lab README on screen and keeps time.
3. Scores go in `games/leaderboards/<game>.md`: one row per player per round. Commit and push to a shared
   fork, or paste rows into a chat and let the host update the file.
4. Debates and card games need a judge: the players not arguing, or an AI agent given the game's
   `rubric.md` and `solution/` notes (it should score arguments, not pick favourites).
5. Pair activities later in the course (resolver relay, red team / blue team) need two machines on one
   network: the host runs `bin/course ref up`; the other player uses the host's IP instead of localhost.

| Module | Group activity | Players | Time |
|---|---|---|---|
| 0 | jshell speedrun | 2+ | 15 min |
| 0 | GitHub API scavenger hunt | 2+ | 15 min |
| 1 | Card sort (lab 1.1) | 2–6 | 15 min |
| 1 | Ranking GitHub's reasons (lab 1.5) | 2+ | 10 min |
| 1 | Latency lab: vote on the clearest page | 2+ | 45 min + 10 |
| 1 | REST or GraphQL? cards | 3–8 | 30 min |
| 1 | Debate: bank goes GraphQL-first | 4+ | 25 min |
| 1 | Case-study teardown: one company each, vote on the best lesson | 2+ | 40 min |
