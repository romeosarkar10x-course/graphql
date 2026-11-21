package dev.shelfie.load;

import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

/**
 * Sends contract/dashboard.graphql as an open workload: -Drate users per second (default 20) for
 * -Dseconds (default 20), each sending one dashboard. Check the status and the data, and assert no
 * failed requests (README).
 */
public class DashboardSimulation extends Simulation {
    static final String URL = System.getProperty("url", "http://localhost:4000/graphql");
    static final int RATE = Integer.getInteger("rate", 20), SECONDS = Integer.getInteger("seconds", 20);

    HttpProtocolBuilder protocol = http.baseUrl(URL).contentTypeHeader("application/json");

    {
        // setUp(...)
    }
}
