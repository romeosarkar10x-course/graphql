# Game 11.9: Multiplayer tic-tac-toe

Lecture: [11.3 Implementing subscriptions](../../lectures/11.3-implementing.md). Grading: auto. Needs nothing.

Two players, moves as mutations, the board as a subscription, spectators welcome. The contract is
`contract/tictactoe.graphqls`; its header lists the error codes for refused moves.

The starter creates and joins matches but accepts any move from anyone, never notices a win, and its
`matchUpdated` subscription starts silent (a spectator sees nothing until the next move), never ends and
counts nobody. Make it a fair game:

1. Refuse every illegal move with its code: `NOT_A_PLAYER`, `WAITING_FOR_OPPONENT`, `NOT_YOUR_TURN`,
   `BAD_CELL`, `CELL_TAKEN`, `GAME_OVER`.
2. Detect a win (and its line) and a draw.
3. `matchUpdated` sends the current board first, then every change, and completes when the match ends; a
   subscriber after the end gets the final board and `complete`.
4. `Match.spectators` counts open subscriptions of people who are not playing (`as` is the subscriber's
   name), and drops when they disconnect.

## Solo

```bash
bin/course start m11/09
bin/course test m11/09
```

Then play yourself in two terminals and watch from a third, inside one `bin/course shell`:
`java -cp "$(ls /course/.cache/course-lib/*.jar | grep -v console | paste -sd:)" /course/modules/m11-realtime/contract/Ttt.java http://localhost:4000/graphql t1 Ada`
(and `Ben`, and `Cleo --watch`), after `mutation { newMatch { id } }`.

## Group

Host the server with `COURSE_PUBLISH=4000 bin/course shell -c "cd workspace/m11-realtime/09-tic-tac-toe &&
mvn -q package && java -jar target/shelfie-ttt.jar"`. Run a round-robin tournament: every pair plays two
matches (each starts once as X), everyone else watches with `--watch`. Before each match, the watchers
try one illegal move against the server (somebody else's turn, a taken cell); a move the server accepts
that it should not is a bug, and its finder scores 1. Record results in
`games/leaderboards/tic-tac-toe.md`.

## Done when

`bin/course test m11/09` passes.
