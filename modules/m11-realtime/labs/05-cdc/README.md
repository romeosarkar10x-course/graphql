# Lab 11.5: Alternatives to subscriptions

Lecture: [11.5 Alternatives to subscriptions](../../lectures/11.5-alternatives.md). Grading: auto + rubric. Needs
PostgreSQL (`bin/course ref up`; the course database runs with `wal_level=logical`).

The starter is lab 11.3's server with messages moved to PostgreSQL: the table `m11.messages`, recreated
when the server starts. `sendMessage` inserts a row and publishes it to the bus. Messages that other
programs write to the table (an import, a moderation tool, a batch job) never reach a subscriber.

Make the database the source of events: use **Debezium's embedded engine** (dependencies already in the
`pom.xml`) to read PostgreSQL's logical replication stream for `m11.messages` and publish every
**inserted** row as a `messageSent` event, whoever wrote it. Each message must still reach a subscriber
exactly once, and an `UPDATE` is not a new message.

```bash
bin/course start m11/05
cd workspace/m11-realtime/05-cdc
bin/course shell -c "mvn -q package && (java -jar target/shelfie-live.jar > /tmp/app.log &) && sleep 15 && \
  (java /course/modules/m11-realtime/contract/Ws.java ws://localhost:4000/graphql \
     'subscription { messageSent(roomId: \"r1\") { author body } }' --seconds 10 &) && sleep 1 && \
  java -cp \"\$(ls /course/.cache/course-lib/*.jar | grep -v console | paste -sd:)\" \
     /course/modules/m11-realtime/contract/Sql.java \"INSERT INTO m11.messages (room_id, author, body) VALUES ('r1', 'psql', 'hi')\" && sleep 3"
```

**The experiment:** `contract/Load.java` runs 500 clients against one room, either polling
`messages(after:)` every 2 s or subscribed, while it sends one message a second (`--send-every 1000`) or
one every 10 s (`--send-every 10000`); `idle` sends without clients, for the baseline. Give it the
server's pid (`--pid $(pgrep -f shelfie-live.jar)`) to read its CPU time from `/proc`. Run it in the
server's container, after one warm-up run (the first run pays for the JIT).

## Done when

`bin/course test m11/05` passes (API messages once, SQL inserts delivered, room filter, no event for an
update, queries read the same table) and `notes.md` passes the rubric (`bin/course grade m11/05`).
