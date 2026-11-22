import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * A HorizontalPodAutoscaler (autoscaling/v2) on one Pods metric, simulated over 25 minutes of traffic (Module 16.6).
 *
 *   bin/course java modules/m16-production/contract/HpaSim.java k8s/hpa.yaml <pod capacity, cost/s> [--trace]
 *
 * The algorithm, from the Kubernetes documentation (concepts/workloads/autoscaling/horizontal-pod-autoscale):
 * every 15 s, desired = ceil(ready pods * average metric per ready pod / target averageValue); no change when the ratio is
 * within 0.1 of 1; at most max(4 pods, 100%) more per 15 s; scale-down takes the highest recommendation of the
 * last 300 s (behavior.scaleDown.stabilizationWindowSeconds overrides it); clamped to [minReplicas, maxReplicas].
 * The simulation adds what a cluster adds: a new pod serves traffic 30 s after it is created.
 *
 * Traffic, as multiples of one pod's capacity: 1.5 for 5 min, a ramp to 6 over 5 min, 6 for 5 min, a drop to 1.5,
 * then 1.5 for 10 min. A pod is overloaded when its share of the traffic exceeds the capacity. Prints one JSON line:
 *   {"target", "minReplicas", "maxReplicas", "peakReplicas", "finalReplicas", "overloadedSeconds", "podSeconds"}
 */
public class HpaSim {
    public static void main(String[] a) throws Exception {
        JsonNode hpa = null;
        try (var it = new YAMLMapper().readValues(new YAMLMapper().createParser(new File(a[0])), JsonNode.class)) {
            while (it.hasNext()) {
                JsonNode d = it.next();
                if (d != null && "HorizontalPodAutoscaler".equals(d.path("kind").asString(""))) hpa = d;
            }
        }
        if (hpa == null) throw new IllegalArgumentException("no HorizontalPodAutoscaler in " + a[0]);
        double capacity = Double.parseDouble(a[1]);
        boolean trace = a.length > 2 && a[2].equals("--trace");
        JsonNode spec = hpa.path("spec");
        int min = spec.path("minReplicas").asInt(1), max = spec.path("maxReplicas").asInt();
        JsonNode metric = spec.path("metrics").path(0);
        double target = quantity(metric.path("pods").path("target").path("averageValue").asString(
                metric.path("pods").path("target").path("averageValue").toString()));
        int window = spec.path("behavior").path("scaleDown").path("stabilizationWindowSeconds").asInt(300);

        int replicas = min, peak = min;
        Deque<int[]> starting = new ArrayDeque<>();   // {seconds until ready, count}
        int ready = min;
        Deque<int[]> recommendations = new ArrayDeque<>();   // {time, desired}
        long overloaded = 0, podSeconds = 0;
        for (int t = 0; t < 25 * 60; t++) {
            double load = capacity * (t < 300 ? 1.5 : t < 600 ? 1.5 + 4.5 * (t - 300) / 300.0 : t < 900 ? 6 : 1.5);
            for (int[] s : starting) s[0]--;
            while (!starting.isEmpty() && starting.peekFirst()[0] <= 0) ready += starting.pollFirst()[1];
            if (load / ready > capacity) overloaded++;
            podSeconds += replicas;
            if (t % 15 == 0) {
                double perPod = load / ready, ratio = perPod / target;
                int desired = Math.abs(ratio - 1) <= 0.1 ? replicas : (int) Math.ceil(ready * ratio);
                desired = Math.max(min, Math.min(max, desired));
                recommendations.addLast(new int[] {t, desired});
                while (recommendations.peekFirst()[0] < t - window) recommendations.pollFirst();
                if (desired < replicas) desired = recommendations.stream().mapToInt(r -> r[1]).max().orElse(desired);
                if (desired > replicas) desired = Math.min(desired, replicas + Math.max(4, replicas));
                if (desired > replicas) { starting.addLast(new int[] {30, desired - replicas}); }
                else if (desired < replicas) { ready -= replicas - desired; }
                replicas = desired;
                peak = Math.max(peak, replicas);
                if (trace) System.err.printf("t=%4ds load=%7.0f ready=%2d replicas=%2d%n", t, load, ready, replicas);
            }
        }
        System.out.printf("{\"target\":%.1f,\"minReplicas\":%d,\"maxReplicas\":%d,\"peakReplicas\":%d,\"finalReplicas\":%d,\"overloadedSeconds\":%d,\"podSeconds\":%d}%n",
                target, min, max, peak, replicas, overloaded, podSeconds);
    }

    /** A Kubernetes quantity: "1500", "1.5k", "1500m" (milli), "2M". */
    static double quantity(String q) {
        q = q.replace("\"", "").trim();
        if (q.endsWith("m")) return Double.parseDouble(q.substring(0, q.length() - 1)) / 1000;
        if (q.endsWith("k")) return Double.parseDouble(q.substring(0, q.length() - 1)) * 1000;
        if (q.endsWith("M")) return Double.parseDouble(q.substring(0, q.length() - 1)) * 1_000_000;
        return Double.parseDouble(q);
    }
}
