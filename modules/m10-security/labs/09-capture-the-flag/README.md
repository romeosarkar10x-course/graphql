# Game 10.9: Capture the flag

Lectures: all of Module 10. Grading: auto (8 of 12 flags). Needs nothing.

Twelve flags (`FLAG{name-xxxxxxxx}`) are hidden in the vulnerable Shelfie fork, one behind each kind of
mistake this module covers. Among them: steal a private review, enumerate users, crash a node (or come
close: one request worth 100,000 field fetches), bypass a role, forge a token, and get past a "persisted
queries only" rule. Some you may already have from lab 10.3.

```bash
bin/course start m10/09
bin/course shell
  /course/modules/m10-security/contract/vulnerable.sh start         # :4000; GraphiQL at /graphiql
  # sign in as the player: mutation { login(username: "ben", password: "ben-pw") }
bin/course test m10/09                                                # checks workspace/.../flags.txt
```

Rules: black box. Use the API, GraphiQL, curl, Hurl, `attack.sh`, your own Java; do not read
`contract/vulnerable-shelfie/src` or this lab's `solution/`. The fork's key is new at every start, so
`bin/course token` cannot help, and restarting resets its data. Subscriptions are at `ws://localhost:4000/graphql`
(graphql-transport-ws; lab 10.7's `tests/WsProbe.java` is a starting point).

## Solo

Find 8 of 12 within the module (pass), all 12 for the leaderboard. Record your score and time in
`games/leaderboards/capture-the-flag.md`.

## Group (2 to 6 players)

Everyone runs their own fork. Two hours, with a scoreboard on a shared screen: first to each flag gets 3
points, everyone after gets 1. At the end, each flag's first finder explains the vulnerability and the fix
in two minutes. Then play lab 10.10 (red team / blue team) with the same people.
