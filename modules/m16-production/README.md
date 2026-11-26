# Module 16 — Running GraphQL in production

About 8 hours, 7 lectures. Shelfie gets observed (metrics by operation, client and field; traces per field),
measured (field usage in Hive's format), governed (owners per type, deprecate → measure → remove), deployed
(probes, graceful shutdown, a rolling deploy with 200 subscriptions), hardened (timeouts, breaker, bulkhead),
sized (a cost metric, an autoscaler on it, the knee) and published (dated versions, changelog, migration guide).

## Before you start

`bin/course ops` downloads the observability stack once into `.cache/ops` (checksummed, about 670 MB, 1.6 GB
unpacked): **Prometheus 3.16.0**, **Tempo 3.1.1** and **Grafana 13.3.0**, Linux x86_64 builds that run inside
the course image. Tests start and stop them on their own (`"needs": ["ops"]`). Grafana runs with anonymous admin,
with usage reporting and update checks off. To look at it yourself, publish its port:
`COURSE_PUBLISH=3000 bin/course shell`, then `ops_start` (below) and open http://localhost:3000.

Shared material in [contract/](contract/):

- `ops-app/`: the reference Shelfie in production shape (Spring Boot 4.2, Spring for GraphQL 2.1), one class per
  production feature: `Observability` (16.1), `UsageReporter` (16.2), `LiveController` + `Shutdown` (16.4),
  `ReviewsClient` (16.5), `CostMetric` (16.6). Every lab's starter is this app minus one feature. `GameDay` is
  the game's faults (16.8; spoilers).
- `ReviewsService.java`: the reviews dependency on :4100 (`/_fault?mode=ok|slow|error`, `/_stats`, a long-poll
  event feed for subscriptions).
- `ops.sh`: test helpers: `ops_start [tempo] [prometheus] [grafana]`, `reviews_start`, `reviews_fault`,
  `promq`, `traceql`, `tempo_trace`, `otel_agent` (the OpenTelemetry Java agent 2.33.1 from Maven Central).
  `ops/` holds the configs.
- Load and traffic: `Peak.java` (open-model load), `ramp.sh` (the knee), `Traffic.java` (seven simulated days),
  `Subscribers.java` (200 resuming subscribers plus queries), `GameTraffic.java` + `gameday.sh` (game 16.8).
- Stand-ins for what a laptop has no room for: `UsageCollector.java` (a registry's usage endpoint, Hive report
  v2), `ReadyBalancer.java` (a readiness-aware balancer with Kubernetes-like propagation delay), `rollout.sh`
  (a rolling deploy), `HpaSim.java` (the HPA algorithm), `Yaml.java` (manifests to JSON).
- `versions/`: the API's dated schemas `2026-10` and `2027-01` (16.7).

## Lectures and labs

| # | Lecture | Lab | Grading |
|---|---|---|---|
| 16.1 | [Observability](lectures/16.1-observability.md) | [m16/01 metrics by operation and client, traces per field; the slowest field from traces](labs/01-observability/README.md) | auto + rubric |
| 16.2 | [Registry and field analytics](lectures/16.2-registry-usage.md) | [m16/02 Hive-format usage reports from an Instrumentation; fields unused in 7 days](labs/02-field-usage/README.md) | auto |
| 16.3 | [Governance](lectures/16.3-governance.md) | [m16/03 owners per type, a review template, remove a deprecated field with usage as proof](labs/03-governance/README.md) | auto + rubric |
| 16.4 | [Deployment](lectures/16.4-deployment.md) | [m16/04 probes, drain, resume; a rolling deploy with 200 subscriptions; Jib and a manifest](labs/04-deployment/README.md) | auto + rubric |
| 16.5 | [Reliability](lectures/16.5-reliability.md) | [m16/05 timeouts, breaker, bulkhead; kill the reviews service at peak](labs/05-reliability/README.md) | auto |
| 16.6 | [Capacity and cost](lectures/16.6-capacity.md) | [m16/06 a cost metric, an HPA on it, the knee before and after an optimisation](labs/06-capacity/README.md) | auto + rubric |
| 16.7 | [GraphQL as a public product](lectures/16.7-public-product.md) | [m16/07 changelog, migration guide and announcement for a dated version](labs/07-public-product/README.md) | auto + rubric |

Labs build on one another's ideas, not their code: each starter is the reference app minus that lab's feature.
16.2 feeds 16.3 (usage data); 16.1, 16.5 and 16.6 feed game 16.8 and mini-project 16.10.

## Mini-projects and games

| | Solo | Group |
|---|---|---|
| [m16/08 Incident game day](labs/08-incident-game-day/README.md) (game) | a random seed, three faults, find them from traces and metrics only, against the clock | a game master picks the seed and shares Grafana; teams split traces, metrics and timeline |
| [m16/09 Governance council](labs/09-governance-council/README.md) (game) | play every seat; write the case for and against each proposal, then decide | chair, proposers, client representatives, API product and SRE decide four proposals in 60 minutes |
| [m16/10 GraphQL ops dashboard](labs/10-project-dashboard/README.md) (mini-project) | a Grafana dashboard (top operations, errors by code, slowest fields, cost per client) and the on-call runbook | one person per incident's panels, merged; swap runbooks and run them on a seed you did not play |

## Spec and sources

- [Shopify API versioning](https://shopify.dev/docs/api/usage/versioning) (read 2026-11-22: quarterly dated
  versions, at least 12 months of support, nine months of overlap, fall forward);
  [Shopify's GraphQL-first API strategy](https://www.shopify.com/partners/blog/all-in-on-graphql)
- [Netflix's federated platform (InfoQ)](https://www.infoq.com/articles/federated-GraphQL-platform-Netflix)
- Hive's usage report v2 schema: `packages/services/usage/usage-report-v2.schema.json` in
  [graphql-hive/console](https://github.com/graphql-hive/console)
- [Kubernetes: Horizontal Pod Autoscaling](https://kubernetes.io/docs/concepts/workloads/autoscaling/horizontal-pod-autoscale/)
- [Resilience4j](https://resilience4j.readme.io/), [Jib](https://github.com/GoogleContainerTools/jib),
  [OpenTelemetry Java agent](https://opentelemetry.io/docs/zero-code/java/agent/)

## Changes from the plan

- **No Kubernetes cluster in the labs.** A cluster does not fit in the course image. The rolling deploy runs two
  replicas behind `ReadyBalancer`, which mimics a Service: readiness-aware, 1 s propagation, no retry. The HPA is
  the algorithm from the Kubernetes documentation in `HpaSim.java`. The tests check the manifests as YAML. The
  course applied them once to a real cluster (kind 0.33.0, Kubernetes 1.37.0, 2026-11-24;
  `_workbench/2026-11-24-finish/k8s/`): lab 16.4's Deployment rolled twice under four clients querying every 20 ms
  through the Service with 0 failed requests (2,425 and 2,579), and lab 16.6's HPA, fed by Prometheus and
  prometheus-adapter 0.12.0, scaled 2 to 12 replicas in 45 s on `graphql_cost_per_second` and back to 2 about
  5.5 minutes after the load stopped (the default 300 s scale-down window).
- **Usage is reported to a stand-in, not to Hive.** Your `Instrumentation` speaks Hive's report format, version 2,
  with Hive's headers, but there is no Hive account. `UsageCollector.java` checks the same shape. Point
  `shelfie.usage.endpoint` at Hive with a real token to go live. GraphOS and Cosmo are lecture-only.
- **Prometheus, Tempo and Grafana run as processes in the course image**, not as course services, so they only use
  CPU while a test or you need them. Tempo's search lags 20 to 30 s, so tests read traces by id.
- **CRaC is lecture-only** (it needs a JDK built with CRaC). Native images remain `bin/course native`
  (Module 6). Jib is configured and checked; `jib:buildTar` needs internet for the base image.
- **The knee is measured on one pinned CPU** (`taskset`), the way a pod's CPU limit would hold the app. A 24-core
  machine showed no knee before the load generator ran out.
- **Request cancellation** (16.5) points back to Module 8's `ExecutionInput.cancel()` and is not graded here.

## Checkpoint

[checkpoint.md](checkpoint.md): `bin/course checkpoint 16` checks labs 16.1, 16.4, 16.5, 16.6 and 16.10 and tags
`checkpoint-16`: a deployed, observable Shelfie with a runbook and a dashboard.
