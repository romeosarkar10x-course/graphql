import java.util.List;
import java.util.Map;

/**
 * Lab 1.6: implement the page-load model below. Tests: bin/course test m01/06
 *
 * Page-load model. A page is a set of calls; each call may depend on others.
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

    /** Wave number (1-based) of every call: 1 + the deepest dependency's wave.
     *  Throws IllegalArgumentException on a cycle or a dependency that is not a key of deps. */
    public static Map<String, Integer> waves(Map<String, List<String>> deps) {
        throw new UnsupportedOperationException("TODO");
    }

    /** Number of sequential round trips: each wave needs ceil(n / maxParallel). */
    public static int roundTrips(Map<String, List<String>> deps, int maxParallel) {
        throw new UnsupportedOperationException("TODO");
    }

    public static double pageMs(Map<String, List<String>> deps, Map<String, Integer> bytes, Network net) {
        throw new UnsupportedOperationException("TODO");
    }
}
