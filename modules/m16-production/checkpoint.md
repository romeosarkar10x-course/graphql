# Checkpoint 16

`bin/course checkpoint 16` checks labs 16.1 (observability), 16.4 (deployment), 16.5 (reliability), 16.6
(capacity) and mini-project 16.10 (dashboard and runbook), and that `RUNBOOK.md` and `dashboard.json` are in your
workspace, then tags `checkpoint-16`. The written parts are graded separately (`bin/course grade m16/NN`).
Answer these first, without looking.

1. Every request is `POST /graphql` with status 200. Which three labels turn that into a useful dashboard, and why
   must one of them be bounded?
2. Spring's default field spans say `graphql field similar`. What is missing, and where does `Book.reviews` show
   up instead?
3. A usage report says nobody selected `Subscription.reviewAdded` in 7 days. Why is that not a reason to remove it?
4. Two deprecated fields: one used only by a retired client version, one used 22 times this week by a supported
   one. What do you do with each, and what goes in the changelog?
5. A rolling deploy drops 1,318 subscription messages. Which change brings that to zero, and why can graceful
   shutdown alone not do it?
6. With resume in place, queries still fail during the deploy. Name the two causes and their fixes.
7. The reviews service takes 2 s per call. Which defence keeps p95 low, which spares the service, and what does
   the client receive for `Book.reviews`?
8. Why scale on cost per second rather than CPU, and why not set the target at the measured capacity?

<details><summary>Answers</summary>

1. Operation name, client name and field coordinate (`Type.field`). The client name comes from a header: unbounded,
   any caller could create a new time series per request, so unknown names become `other`.
2. The parent type (the coordinate), so a custom `DataFetcherObservationConvention` names spans by `Type.field`.
   `Book.reviews` is a batch mapping: its time is the `graphql dataloader` span and the HTTP call under it.
3. Subscriptions were not in the reported traffic at all: what is not reported always looks unused. Hive reports
   them separately (`subscriptionOperations`).
4. Remove the first (one breaking change, checked with a schema diff, with the evidence and a rollback); keep the
   second deprecated until its supported client stops using it. The changelog has a dated entry for the removal
   and says why the other stays.
5. A resume cursor: clients pass `after: <last seq>` and the server serves what they missed, then the live feed
   (listen first, then fetch, dropping by `seq`). Messages published while a client reconnects reach no socket,
   whatever the shutdown order.
6. The balancer still sends new connections for a moment after readiness flips: flip readiness first, then drain
   longer than it takes to notice. Kept-alive connections bypass the balancer: answer with `Connection: close`
   while draining.
7. A short read timeout and the circuit breaker keep latency low (the breaker answers at once while open); the
   breaker (and the bulkhead) spare the service. The client gets `reviews: null` with an error whose
   `extensions.code` is `REVIEWS_UNAVAILABLE` at that path; the rest of the page renders.
8. Cost moves with the expensive queries before CPU does, and it is known before execution. New pods take time to
   serve: at 98% of capacity the simulated ramp outran the scaler for 214 s; at 75% for 15 s.

</details>
