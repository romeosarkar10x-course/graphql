import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Page-load model for lab 1.6. A page is a set of calls; each call may depend on others.
 * Calls whose dependencies are all done go out together as one "wave", at most maxParallel at a time.
 *
 *   pageMs = sum over waves of ceil(callsInWave / maxParallel) * (rttMs + serverMs)
 *          + totalBytes * 8 / kbps
 */
public final class LatencyModel {

    public record Network(int rttMs, int serverMs, double kbps, int maxParallel) {
        public Network {
            if (rttMs < 0 || serverMs < 0) throw new IllegalArgumentException("negative time");
            if (kbps <= 0) throw new IllegalArgumentException("kbps must be > 0");
            if (maxParallel <= 0) throw new IllegalArgumentException("maxParallel must be > 0");
        }
    }

    private LatencyModel() {}

    /** Wave number (1-based) of every call: 1 + the deepest dependency's wave. */
    public static Map<String, Integer> waves(Map<String, List<String>> deps) {
        Map<String, Integer> wave = new HashMap<>();
        for (String call : deps.keySet()) depth(call, deps, wave, new java.util.HashSet<>());
        return wave;
    }

    private static int depth(String call, Map<String, List<String>> deps, Map<String, Integer> wave, java.util.Set<String> path) {
        Integer known = wave.get(call);
        if (known != null) return known;
        if (!deps.containsKey(call)) throw new IllegalArgumentException("unknown call: " + call);
        if (!path.add(call)) throw new IllegalArgumentException("dependency cycle through " + call);
        int d = 1;
        for (String dep : deps.get(call)) d = Math.max(d, 1 + depth(dep, deps, wave, path));
        path.remove(call);
        wave.put(call, d);
        return d;
    }

    /** Number of sequential round trips: each wave needs ceil(n / maxParallel). */
    public static int roundTrips(Map<String, List<String>> deps, int maxParallel) {
        if (maxParallel <= 0) throw new IllegalArgumentException("maxParallel must be > 0");
        Map<Integer, Integer> perWave = new HashMap<>();
        waves(deps).values().forEach(w -> perWave.merge(w, 1, Integer::sum));
        return perWave.values().stream().mapToInt(n -> (n + maxParallel - 1) / maxParallel).sum();
    }

    public static double pageMs(Map<String, List<String>> deps, Map<String, Integer> bytes, Network net) {
        long totalBytes = 0;
        for (String call : deps.keySet()) totalBytes += bytes.getOrDefault(call, 0);
        return roundTrips(deps, net.maxParallel()) * (double) (net.rttMs() + net.serverMs())
                + totalBytes * 8.0 / net.kbps();
    }
}
