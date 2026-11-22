# Lab 12.8: Client resilience

Lecture: [12.8 Client resilience](../../lectures/12.8-client-resilience.md). Grading: auto + rubric.
Needs the reference server and Toxiproxy (`bin/course ref up` starts both).

`shelfie-resilient` is a command-line client on `HttpSyncGraphQlClient` with Resilience4j in the pom.
`Shelfie.java` already has timeouts (500 ms to connect and to read): without them one hung connection hangs
the caller forever and no retry ever runs. Make it survive a bad network:

```bash
java -jar target/shelfie-resilient.jar <url> load 200 [--plain]       # ok 199/200, failed 1, circuit open 0, 9545 ms
java -jar target/shelfie-resilient.jar <url> reviews u11 b1 b2 ...    # b1 r16 created / b2 r17 recovered
```

1. **Retries with backoff** (`Resilient.call`) for queries: 97% of 200 must succeed through 30% lost responses
   and 5% hung connections, where `--plain` (one attempt) gets about 66%.
2. **A circuit breaker**: with Shelfie unreachable, 80 or more of 100 calls must be refused at once
   (`circuit open`), the whole run under 15 s.
3. **Safe mutation replays**: `createReview` for ten books through lost responses. Shelfie must end up with
   exactly one review per book, and your client must print every review's real id. A lost response is not a
   failed mutation: the server may have stored it.

The test creates its own Toxiproxy proxy (`toxiproxy:8667`, removed afterwards) and runs your client with
`-Djdk.httpclient.keepalive.timeout=0`; to try it by hand:

```bash
bin/course start m12/08
cd workspace/m12-clients/08-resilience
bin/course shell -c 'T=http://toxiproxy:8474
  curl -s -XPOST $T/proxies -d "{\"name\":\"try\",\"listen\":\"0.0.0.0:8668\",\"upstream\":\"shelfie-ref:8080\"}"
  curl -s -XPOST $T/proxies/try/toxics -d "{\"type\":\"reset_peer\",\"stream\":\"downstream\",\"toxicity\":0.3,\"attributes\":{\"timeout\":0}}"
  mvn -q package && java -Djdk.httpclient.keepalive.timeout=0 -jar target/shelfie-resilient.jar http://toxiproxy:8668/graphql load 200 --plain
  curl -s -XDELETE $T/proxies/try'
```

In `notes.md`: before and after numbers and why, what is safe to retry, your breaker's settings, and what a
client does with persisted documents.

## Done when

`bin/course test m12/08` passes (both load runs, the ten reviews, the breaker) and `notes.md` passes the rubric
(`bin/course grade m12/08`).
