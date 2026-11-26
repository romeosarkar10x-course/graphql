# Deploying Shelfie

## The experiment: messages dropped during a rolling deploy

Two replicas behind `ReadyBalancer` (1 s for a readiness change to take effect, no retry on a refused connection),
200 subscribers, one review every 50 ms (about 490 in 25 s), four clients querying every 20 ms; each replica
stopped with SIGTERM and started again in turn.

| Version | Dropped | Duplicates | Failed queries |
| --- | --- | --- | --- |
| starter + probes only (no resume, no drain) | 1,318 (second run 1,429) | 0 | 4 of 4,184 |
| + `after` resume | 0 | 0 | 6 of 4,103 |
| + readiness first, 1 s drain | 0 | 0 | 1 of 4,164 |
| + 2 s drain and `Connection: close` while draining (this solution) | 0 | 0 | 0 of 4,166 |

## What each change bought

- **Probes**: without `/actuator/health/readiness` the balancer never sends anything to a replica. In Kubernetes
  the probes are what makes a rolling update wait for the new pod.
- **Resume** removed every dropped message. Each client reconnects about 300 times in total over the two
  restarts, and before resume every reconnect lost what was published during it. Graceful shutdown cannot fix
  that: a subscription does not "finish", so the only cure is a cursor the client brings back.
- **Readiness before stopping, and a drain longer than the balancer's delay**: Boot flips readiness when the
  context closes, but the port closed almost at once, while the balancer still sent new connections there for up
  to a second. With a 1 s drain one query still lost the race (1 s propagation plus a 100 ms probe period), so
  the drain is 2 s.
- **`Connection: close` while draining**: clients reuse kept-alive connections, which go straight to the old
  replica whatever the balancer believes. Asking them to close makes their next request open a new connection,
  which lands on a ready replica.
- **Completing subscriptions** on shutdown makes clients reconnect at once instead of noticing a dead socket. Each
  replica still took 9 to 11 s to stop; 2 s of that is the drain, and I did not measure where the rest goes.

The Kubernetes manifest matches: `maxUnavailable: 0`, readiness `periodSeconds: 2` and
`terminationGracePeriodSeconds: 30`, which is more than the drain plus the 20 s shutdown phase.

Applied to a real cluster once (kind 0.33.0, Kubernetes 1.37.0, 2026-11-24, `_workbench/2026-11-24-finish/k8s/`):
`kubectl rollout restart` with four clients querying the Service every 20 ms (a new connection each, curl in a
pod), twice: 0 failed of 2,425 and 0 of 2,579 requests, the rollout reported done in 7 s. The image was Jib's
`jib:buildTar`, loaded with `kind load docker-image`.

## Native image and CRaC: would you use them here?

Both shorten start-up, which matters for scaling out quickly, not for this rolling deploy: here the start took
about 2 s of a 10 s cycle. A GraalVM native image (`bin/course native -Pnative native:compile`, as in Module 6)
starts in a fraction of a second and uses less memory. It costs long builds, reflection hints for anything
dynamic, and the loss of the JIT's peak throughput. CRaC restores a JVM from a checkpoint taken after warm-up.
That needs a JDK with CRaC (not the course image's Temurin) and care with open sockets and secrets in the
snapshot; I did not run it. For Shelfie I would not adopt either yet: start-up is not the bottleneck, and Jib's
plain JVM image is simpler to operate.
