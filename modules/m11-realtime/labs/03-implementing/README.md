# Lab 11.3: Implementing subscriptions

Lecture: [11.3 Implementing subscriptions](../../lectures/11.3-implementing.md). Grading: auto + rubric. Needs nothing.

The starter is lab 11.2's server (no client) with a "someone is typing" indicator that works in a demo
and fails in a room: `typing(roomId, me)` and the `typing` mutation, a course-only `/_course/flood`
endpoint for the experiment, and `Room.watching` (open subscriptions on a room). All the work is in
`LocalBus.java`:

1. **Per-subscriber filter.** `typing(roomId, me: "Ada")` echoes Ada's own typing back to her.
2. **Backpressure.** A typing indicator needs "who is typing now", not every keystroke. Under a flood of
   10,000 events a second, a client must get at most a few events per author per 250 ms, and a slow
   client must never queue up stale ones.
3. **Cleanup.** `Room.watching` never goes down when a client just disconnects.
4. **`takeUntil`.** `archiveRoom` should complete every open subscription on that room.

```bash
bin/course start m11/03
cd workspace/m11-realtime/03-implementing
# the experiment, all in one container:
bin/course shell -c "mvn -q package && (java -Xmx256m -jar target/shelfie-live.jar > /tmp/app.log &) && sleep 8 && \
  (java /course/modules/m11-realtime/contract/Ws.java ws://localhost:4000/graphql \
     'subscription { typing(roomId: \"r2\", me: \"Ben\") { author } }' --read-rate 20 --seconds 15 > /tmp/slow &) && \
  sleep 1 && curl -s -X POST 'localhost:4000/_course/flood?roomId=r2&perSecond=10000&seconds=3&authors=3' && \
  sleep 14 && grep -c next /tmp/slow && tail -2 /tmp/slow"
```

`--read-rate 20` makes `Ws.java` read only 20 frames a second: a phone on a bad network. Run the
experiment before and after your fix, and watch when the slow client's last event arrives.

## Done when

`bin/course test m11/03` passes (the filter, the flood with a fast and a slow client, the server's
survival, cleanup on disconnect, completion on archive) and `notes.md` passes the rubric
(`bin/course grade m11/03`).
