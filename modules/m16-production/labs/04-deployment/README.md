# Lab 16.4: Deployment: probes, graceful shutdown, a rolling deploy with 200 open subscriptions

Lecture: [16.4 Deployment](../../lectures/16.4-deployment.md). Grading: auto + rubric.

Roll a new version while 200 clients hold subscriptions, and lose nothing. The test plays Kubernetes in
miniature:

- two replicas of your jar run on :4001 and :4002;
- `contract/ReadyBalancer.java` on :4000 sends new connections only to replicas whose
  `/actuator/health/readiness` answers 200. A change takes **1 s** to take effect, as endpoint updates do in a
  cluster, and a refused connection is **not** retried;
- `contract/Subscribers.java` opens 200 subscriptions to `reviewAdded(after: $after) { seq }`. A writer adds a
  review every 50 ms, and four clients query every 20 ms;
- each replica in turn gets SIGTERM, exits, starts again and becomes ready.

When a subscription ends or its socket closes, a client reconnects and passes the last `seq` it saw as `after`.
The starter ignores `after` and has no probes. By the end of the deploy:

- every subscriber has every message, and none twice;
- no query has failed;
- every subscriber is subscribed again.

Then package it:

- **Jib** in `pom.xml`: a base image, port 4000 and a non-root user. `mvn package jib:buildTar` writes an image
  tar without a Docker daemon; it needs internet for the base image.
- **`k8s/*.yaml`**, a Deployment and Service with:
  - readiness and liveness probes on the Actuator groups;
  - at least 2 replicas;
  - `maxUnavailable: 0`;
  - a `terminationGracePeriodSeconds` longer than your shutdown takes.

```bash
bin/course start m16/04
cd workspace/m16-production/04-deployment
bin/course test m16/04      # about two minutes: the deploy itself takes ~80 s
```

To watch a deploy by hand, run `source` on `contract/ops.sh` and `contract/rollout.sh` in `bin/course shell`, then
`reviews_start; rollout_run $PWD/target/shelfie-ops.jar 200 25`. The balancer's log is `/tmp/balancer.log`.

## Experiment: count dropped messages

1. Enable only the probes and run the deploy. Note the dropped messages and the failed queries.
2. Add the changes one at a time and run it again after each.
3. Write `DEPLOY.md`:
   - the table of runs;
   - what each change bought, and why;
   - whether a GraalVM native image or CRaC would help this deploy (`bin/course native` builds native images, as in
     Module 6).

## Done when

`bin/course test m16/04` passes and `DEPLOY.md` passes the rubric (`bin/course grade m16/04`).
