import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LatencyModelTest {

    static final LatencyModel.Network FAST_PIPE = new LatencyModel.Network(100, 0, 1e12, 6);

    /** The club page from lab 1.2, without the user cache: 13 calls. */
    static Map<String, List<String>> clubPage() {
        Map<String, List<String>> d = new HashMap<>();
        d.put("club", List.of());
        for (String c : List.of("owner", "m1", "m2", "m3", "m4", "m5", "book", "reviews", "meetups")) d.put(c, List.of("club"));
        for (String c : List.of("rv1", "rv2", "rv3")) d.put(c, List.of("reviews"));
        return d;
    }

    @Test void singleCall() {
        var deps = Map.of("q", List.<String>of());
        assertEquals(1, LatencyModel.waves(deps).get("q"));
        assertEquals(120.0, LatencyModel.pageMs(deps, Map.of(), new LatencyModel.Network(100, 20, 1e12, 6)), 1e-6);
    }

    @Test void chainIsOneWavePerLink() {
        var deps = Map.of("a", List.<String>of(), "b", List.of("a"), "c", List.of("b"));
        assertEquals(Map.of("a", 1, "b", 2, "c", 3), LatencyModel.waves(deps));
        assertEquals(3, LatencyModel.roundTrips(deps, 6));
    }

    @Test void diamondTakesTheDeepestPath() {
        var deps = Map.of("a", List.<String>of(), "b", List.of("a"), "c", List.of("a", "b"));
        assertEquals(3, LatencyModel.waves(deps).get("c"));
    }

    @Test void clubPageWaves() {
        var w = LatencyModel.waves(clubPage());
        assertEquals(1, w.get("club"));
        assertEquals(2, w.get("reviews"));
        assertEquals(3, w.get("rv2"));
    }

    @Test void parallelLimitSplitsAWave() {
        // wave 2 has 9 calls: 2 round trips at 6 in flight, 9 at 1 in flight
        assertEquals(1 + 2 + 1, LatencyModel.roundTrips(clubPage(), 6));
        assertEquals(1 + 9 + 3, LatencyModel.roundTrips(clubPage(), 1));
        assertEquals(3, LatencyModel.roundTrips(clubPage(), 100));
    }

    @Test void pageTimeAddsTransfer() {
        Map<String, Integer> bytes = new HashMap<>();
        clubPage().keySet().forEach(c -> bytes.put(c, 500));     // 13 * 500 = 6,500 bytes = 52,000 bits
        var net = new LatencyModel.Network(700, 10, 1000, 6);  // 1 Mbit/s = 1,000 bits per ms
        assertEquals(4 * 710 + 52.0, LatencyModel.pageMs(clubPage(), bytes, net), 1e-6);
    }

    @Test void graphqlIsOneRoundTrip() {
        var deps = Map.of("graphql", List.<String>of());
        assertEquals(700 + 824 * 8 / 1000.0,
                LatencyModel.pageMs(deps, Map.of("graphql", 824), new LatencyModel.Network(700, 0, 1000, 6)), 1e-6);
    }

    @Test void missingBytesCountAsZero() {
        assertEquals(400.0, LatencyModel.pageMs(clubPage(), Map.of(), FAST_PIPE), 1e-6);
    }

    @Test void cycleIsRejected() {
        var deps = Map.of("a", List.of("b"), "b", List.of("a"));
        assertThrows(IllegalArgumentException.class, () -> LatencyModel.waves(deps));
    }

    @Test void unknownDependencyIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> LatencyModel.waves(Map.of("a", List.of("ghost"))));
    }

    @Test void badNetworkIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new LatencyModel.Network(1, 1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new LatencyModel.Network(1, 1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> LatencyModel.roundTrips(clubPage(), 0));
    }
}
