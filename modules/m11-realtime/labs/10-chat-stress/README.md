# Game 11.10: Classroom chat stress test

Lectures: [11.2](../../lectures/11.2-transports.md) and [11.4](../../lectures/11.4-scaling.md). Grading: auto. Needs Redis.

The starter is lab 11.4's server on Redis pub/sub (`shelfie.bus: redis`) plus lab 11.2's reconnecting
`client/Chat.java`. Fan-out across instances works, until an instance dies: the clients that were on it
reconnect through the balancer to another instance and ask it for `messages(after: <last id>)`, and that
instance has never heard of their ids. Each instance numbers messages with its own counter and keeps
only the messages it accepted.

Make the cluster survive its instructor: ids and history must be the same on every instance, so any
instance can fill any client's gap. Redis is already there (the game uses database 11; the test empties
it first).

The test starts three instances behind `contract/Balancer.java`, connects nine `Chat.java` clients to
room r1 through it, sends 48 messages (one every 250 ms, to the instances in turn, retrying elsewhere when
one is down), kills instance b with `kill -9` after 12, restarts it after 24 and kills c after 30. Every
client must print all 48, exactly once, in order.

## Solo

```bash
bin/course start m11/10
bin/course test m11/10
```

## Group

The instructor runs the three instances and the balancer with `COURSE_PUBLISH=4000 bin/course shell`;
everyone else runs `client/Chat.java http://<instructor>:4000/graphql r1 --seconds 600` and sends messages
with curl to `:4000`. Every few minutes the instructor kills an instance (`kill -9`) or brings one back.
At the end everyone compares their output with the room's history (`messages(roomId: "r1", last: 1000)`):
a missing message costs 2 points, a duplicate 1, and a client that stopped trying loses 5. Record scores in
`games/leaderboards/chat-stress.md`. Then try it on the starter, for contrast.

## Done when

`bin/course test m11/10` passes.
