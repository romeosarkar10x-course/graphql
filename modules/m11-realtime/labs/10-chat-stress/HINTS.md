# Hints: m11/10

1. Two things are per instance in `Store`: the counter that makes message ids, and the list of messages.
   Both have to live where every instance can see them. Run the starter's test and look at which ids each
   client printed.
2. Redis gives you a shared counter (`INCR`, `StringRedisTemplate.opsForValue().increment`) and an ordered
   collection per room (a sorted set scored by the id, `opsForZSet().add(key, json, id)`); `range(key, 0, -1)`
   reads it in id order. The seed messages must be written once per cluster, not once per instance.
3. `opsForValue().setIfAbsent("shelfie:seeded", "1")` returns true for exactly one instance. Store each
   message as JSON with the `JsonMapper` bean and read it back with `readValue(s, Message.class)`. The
   client is already right; if a client still misses one message after a kill, read lab 11.2's notes.
