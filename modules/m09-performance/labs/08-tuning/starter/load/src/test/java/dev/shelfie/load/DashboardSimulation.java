package dev.shelfie.load;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.nothingFor;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * An open workload: new users arrive at a fixed rate whatever the server's latency (a closed loop of
 * N users would slow down with the server and hide the queueing). Each sends the dashboard once.
 *   -Durl=http://localhost:4000/graphql  -Drate=20 (users per second)  -Dseconds=20
 */
public class DashboardSimulation extends Simulation {
    static final String URL = System.getProperty("url", "http://localhost:4000/graphql");
    static final int RATE = Integer.getInteger("rate", 20), SECONDS = Integer.getInteger("seconds", 20);

    static String body() {
        try {
            String q = Files.readString(Path.of("/course/modules/m09-performance/contract/dashboard.graphql"));
            return "{\"query\":\"" + q.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"}";
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }

    HttpProtocolBuilder protocol = http.baseUrl(URL).contentTypeHeader("application/json").acceptHeader("application/json");

    ScenarioBuilder dashboard = scenario("dashboard").exec(http("dashboard").post("").body(StringBody(body()))
        .check(status().is(200), jsonPath("$.data.clubs[9].name").exists(), jsonPath("$.errors").notExists()));

    {
        setUp(dashboard.injectOpen(nothingFor(Duration.ofSeconds(1)), constantUsersPerSec(RATE).during(Duration.ofSeconds(SECONDS))))
            .protocols(protocol)
            .assertions(global().failedRequests().count().is(0L), global().responseTime().percentile(99.0).lt(2_000));
    }
}
