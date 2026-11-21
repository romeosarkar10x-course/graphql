# Lab 11.1: Subscriptions, conceptually

Lecture: [11.1 Subscriptions, conceptually](../../lectures/11.1-subscriptions.md). Grading: auto + rubric. Needs nothing.

The starter is Module 11's Shelfie (`contract/live.graphqls`): chat rooms r1 to r3, `sendMessage`, and a
`Bus` that publishes every message once. Two things are missing:

1. `messageSent(roomId)` never emits. Make it the room's messages, as they are sent; an unknown room is a
   `NOT_FOUND` error.
2. `countdown(from, failAt)` is the experiment: its source stream fails at `failAt`, and the client gets
   Spring's bare `"Subscription error"`. Make the error say what broke, with `extensions.code`
   `SOURCE_STREAM_FAILED`.

```bash
bin/course start m11/01
cd workspace/m11-realtime/01-subscriptions && bin/course shell -c "mvn -q package && java -jar target/shelfie-live.jar"
# in a second terminal: watch the frames
bin/course shell -c "java /course/modules/m11-realtime/contract/Ws.java ws://localhost:4000/graphql \
  'subscription { countdown(from: 5, failAt: 2) }'"
```

`bin/course shell` starts a new container, so the second terminal must reach the first: run both commands
in one `bin/course shell` (start the app with `&`), as the tests do. `contract/Ws.java` prints each frame
with its time; `--vars '{"r":"r1"}'` passes variables. Over SSE:
`curl -N -H 'Content-Type: application/json' -H 'Accept: text/event-stream' -d '{"query":"subscription { countdown(from: 5, failAt: 2) }"}' localhost:4000/graphql`.

In `notes.md`: diagram one chat message from the mutation to every subscriber, paste the frames you got
before and after the fix (WebSocket and SSE), and explain how a source stream error differs from a field
error inside one event.

## Done when

`bin/course test m11/01` passes (three subscribers in two rooms, an unknown room, a countdown that completes
and one that fails) and `notes.md` passes the rubric (`bin/course grade m11/01`).
