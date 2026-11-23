package dev.toolshed.lending;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** The lending subgraph: java -jar lending/target/lending.jar (port 4002; GraphQL over HTTP and WebSocket at /graphql). */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LendingApplication {
    public static void main(String[] args) {
        SpringApplication.run(LendingApplication.class, args);
    }
}
