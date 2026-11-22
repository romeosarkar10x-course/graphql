# Lab 16.6: Capacity and cost: a cost metric, an autoscaler on it, and the knee

Lecture: [16.6 Capacity and cost](../../lectures/16.6-capacity.md). Grading: auto + rubric. Needs `bin/course ops`.

CPU is a poor signal for a GraphQL server: one huge query and a hundred tiny ones look alike until it is too
late. Scale on what requests **cost**.

1. **`CostMetric`** (a graphql-java `Instrumentation`). Count each field selected as 1, times the number of
   items its parents return. A list field returns `first` items when it has that argument; otherwise it returns
   its expected size (the table in `CostMetric.java`). For example,
   `{ books(first: 2) { title similar { title } } }` costs 1 + 2 + 2 + 2×3 = 11. Put it in every response as
   `extensions.cost`, and record it as a `DistributionSummary` named `graphql.cost`, tagged
   `graphql.client.name` and `graphql.operation.name` (`graphql_cost_sum` / `_count` in Prometheus).
2. **`k8s/cost-per-second.promql`**: one PromQL expression for the cost per second of each pod (`instance`),
   over the last 30 s. An adapter (prometheus-adapter, KEDA) would serve it to Kubernetes as
   `graphql_cost_per_second`.
3. **`k8s/hpa.yaml`**: an `autoscaling/v2` HorizontalPodAutoscaler for Deployment `shelfie`, on the Pods metric
   `graphql_cost_per_second` with an `AverageValue` target and at least 2 replicas.
4. **Find the knee.** Pin the app to one CPU, as a pod limit would, and raise the rate until p99 breaks:

   ```bash
   bin/course shell
   source /course/tools/testlib.sh; source /course/modules/m16-production/contract/ops.sh; reviews_start
   taskset -c 0 java -XX:ActiveProcessorCount=1 -jar target/shelfie-ops.jar --management.tracing.enabled=false &
   bash /course/modules/m16-production/contract/ramp.sh 5 100 200 400 600 800 1000
   ```

   Knee rate × the page's cost (`extensions.cost` of `contract/club-page.json`) is your pod capacity. Put it in
   `CAPACITY.md` as a line `Pod capacity: <N> cost/s`. Add one optimisation, find the knee again, and record both.

The test runs `contract/HpaSim.java` (the HPA algorithm from the Kubernetes documentation, with a 30 s pod start)
with your manifest and your capacity, over 25 minutes of traffic that ramps from 1.5 to 6 pods' worth and back:

- at most 60 s with an overloaded pod;
- at most 9,000 pod-seconds;
- back to at most 3 replicas at the end.

Choose the target accordingly. `--trace` prints every decision.

```bash
bin/course start m16/06
cd workspace/m16-production/06-capacity
bin/course test m16/06
```

## Done when

`bin/course test m16/06` passes and `CAPACITY.md` passes the rubric (`bin/course grade m16/06`). The notes cover:
the knee table, the optimisation and the knee after it, the target and the simulation runs that chose it, and
what edge caching and multi-region reads would change.
