# Mini-project 11.8: Live trivia

Lectures: [11.1](../../lectures/11.1-subscriptions.md) to [11.3](../../lectures/11.3-implementing.md). Grading: auto. Needs nothing.

A Kahoot-style quiz on books: the host creates a game, players join, questions are pushed to everyone by
subscription, answers are mutations, and a leaderboard follows each question. The contract is
`contract/trivia.graphqls` (the rules are in its comments) and the questions are `contract/questions.json`.

The starter is a Spring for GraphQL app with the schema, the question bank and the lobby (`createGame`,
`joinGame`, the `game` query) working. Write the game: `startGame`, `answer` with its scoring and refusals,
a timer per question that closes it early when everyone has answered, the `gameEvents` stream (a union:
`QuestionAsked`, `QuestionClosed`, `LeaderboardChanged`, `GameOver`) and its end after `GameOver`.

The test plays a scripted three-question game with three players (one of them never answers the last
question) and a spectator, and checks every event, the refusals, the timer and the final scores.

## Solo

```bash
bin/course start m11/08
bin/course test m11/08
```

Then play against yourself: start the server, `createGame` with curl, open two terminals running
`bin/course java modules/m11-realtime/contract/Play.java http://localhost:4000/graphql g1 <name>`, and
`startGame`. `Play.java` joins, prints each question, reads your choice and shows the leaderboard.

## Group

One person hosts: `COURSE_PUBLISH=4000 bin/course shell -c "cd workspace/m11-realtime/08-project-trivia &&
mvn -q package && java -jar target/shelfie-trivia.jar"` publishes port 4000 on their machine; the others use
`http://<host's address>:4000/graphql`. The host creates a game with `secondsPerQuestion: 20`, and starts it when everyone has joined with `Play.java`
pointed at the host's address. Play all eight questions. Then the host kills the server mid-question and
everyone writes down what their client did: the subscription has no resume, and the game state was in one
JVM's memory, which is lab 11.4's and the chat stress test's problem.

## Done when

`bin/course test m11/08` passes.
