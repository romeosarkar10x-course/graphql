# Lab 11.4: Scaling fan-out

Lecture: [11.4 Scaling fan-out](../../lectures/11.4-scaling.md). Grading: auto + rubric. Needs Redis and Kafka
(`bin/course ref up` starts both; Kafka is `kafka:9092` inside the course image).

The starter is lab 11.3's server, ready to run as several instances. `LocalBus` no longer emits directly:
every event goes to a `Relay` (when `shelfie.bus` is `redis` or `kafka`) and comes back to every instance
through `LocalBus.deliver`. Both relays are broken:

1. `RedisRelay` delivers locally and publishes nothing, so a message reaches only the subscribers of the
   instance that took the mutation. Use Redis pub/sub (`ReactiveStringRedisTemplate.convertAndSend`,
   `ReactiveRedisMessageListenerContainer`).
2. `KafkaRelay` publishes to the topic `shelfie.events` and listens, and yet two thirds of the subscribers
   get nothing. Find out why from how Kafka consumer groups work, and fix it.

The test starts three instances (`--server.port=4001..4003 --shelfie.instance=a|b|c --shelfie.bus=...`)
behind `contract/Balancer.java` on :4000, a round-robin TCP load balancer, connects nine subscribers
through it (three land on each instance), sends three messages to each instance directly, and expects
every subscriber to get all nine.

```bash
bin/course start m11/04
cd workspace/m11-realtime/04-scaling
bin/course shell -c "mvn -q package && for i in 1 2 3; do (java -Xmx256m -jar target/shelfie-live.jar \
  --server.port=400\$i --shelfie.instance=\$i --shelfie.bus=redis > /tmp/r\$i.log &); done; sleep 12; \
  (java /course/modules/m11-realtime/contract/Balancer.java 4000 4001,4002,4003 &); sleep 1; bash"
```

## Done when

`bin/course test m11/04` passes (the spread and the nine-of-nine fan-out, over Redis and over Kafka) and
`notes.md` passes the rubric (`bin/course grade m11/04`).
