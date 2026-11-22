package probe;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.*;
import org.apache.kafka.clients.admin.AdminClient;
import org.junit.jupiter.api.Test;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.*;

class TcTest {
  @Test void postgres() throws Exception {
    long t = System.nanoTime();
    try (var pg = new PostgreSQLContainer("postgres:18.7-alpine")) {
      pg.start();
      System.out.println("PG up in " + (System.nanoTime() - t) / 1_000_000 + " ms at " + pg.getJdbcUrl());
      try (var c = DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
           var rs = c.createStatement().executeQuery("select version()")) { rs.next(); System.out.println(rs.getString(1)); }
    }
  }
  @Test void kafka() throws Exception {
    long t = System.nanoTime();
    try (var k = new KafkaContainer("apache/kafka:4.4.0")) {
      k.start();
      System.out.println("Kafka up in " + (System.nanoTime() - t) / 1_000_000 + " ms at " + k.getBootstrapServers());
      try (var a = AdminClient.create(Map.of("bootstrap.servers", k.getBootstrapServers()))) {
        System.out.println("cluster " + a.describeCluster().clusterId().get());
      }
    }
  }
  @Test void keycloak() {
    long t = System.nanoTime();
    try (var kc = new KeycloakContainer("quay.io/keycloak/keycloak:26.8.2")) {
      kc.start();
      System.out.println("Keycloak up in " + (System.nanoTime() - t) / 1_000_000 + " ms at " + kc.getAuthServerUrl());
    }
  }
}
