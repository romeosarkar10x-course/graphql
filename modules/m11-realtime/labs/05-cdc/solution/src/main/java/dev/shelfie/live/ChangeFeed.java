package dev.shelfie.live;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lecture 11.5: change data capture. Debezium's embedded engine reads PostgreSQL's logical replication
 * stream (pgoutput) for m11.messages and turns every INSERT into a messageSent event, whoever wrote it:
 * this server's mutation, psql, a batch job. The database is the source of truth for "a message was sent".
 */
@Component
class ChangeFeed {
  private static final Logger log = LoggerFactory.getLogger(ChangeFeed.class);
  private final Bus bus;
  private final JsonMapper json;
  private final ExecutorService runner = Executors.newSingleThreadExecutor();
  private final CountDownLatch streaming = new CountDownLatch(1);
  private DebeziumEngine<ChangeEvent<String, String>> engine;

  ChangeFeed(Bus bus, JsonMapper json, Store store) { // Store first: it creates the table
    this.bus = bus;
    this.json = json;
  }

  @PostConstruct
  void start() throws InterruptedException {
    Properties p = new Properties();
    p.setProperty("name", "shelfie-live");
    p.setProperty("connector.class", "io.debezium.connector.postgresql.PostgresConnector");
    p.setProperty("offset.storage", "org.apache.kafka.connect.storage.MemoryOffsetBackingStore");
    p.setProperty("database.hostname", "postgres");
    p.setProperty("database.port", "5432");
    p.setProperty("database.user", "shelfie");
    p.setProperty("database.password", "shelfie");
    p.setProperty("database.dbname", "shelfie");
    p.setProperty("topic.prefix", "shelfie");
    p.setProperty("plugin.name", "pgoutput");
    p.setProperty("table.include.list", "m11.messages");
    p.setProperty("slot.name", "shelfie_m11");
    p.setProperty("slot.drop.on.stop", "true");          // a course server: do not leave a slot holding WAL
    p.setProperty("publication.name", "shelfie_m11");
    p.setProperty("publication.autocreate.mode", "filtered");
    p.setProperty("snapshot.mode", "no_data");           // only changes from now on: the seed is history
    p.setProperty("poll.interval.ms", "50");             // the default (500 ms) shows up as half a second of lag
    p.setProperty("key.converter.schemas.enable", "false");
    p.setProperty("value.converter.schemas.enable", "false");
    engine = DebeziumEngine.create(Json.class).using(p)
        .using(new DebeziumEngine.ConnectorCallback() {
          @Override public void taskStarted() { log.info("change feed: streaming m11.messages"); streaming.countDown(); }
        })
        .notifying(this::onChange).build();
    runner.execute(engine);
    if (!streaming.await(60, TimeUnit.SECONDS)) log.warn("change feed: not streaming after 60 s");
  }

  private void onChange(ChangeEvent<String, String> e) {
    if (e.value() == null) return;                       // a tombstone after a DELETE
    JsonNode v = json.readTree(e.value());
    if (!"c".equals(v.path("op").asString())) return;    // inserts only: c(reate), not u(pdate) or d(elete)
    JsonNode row = v.path("after");
    bus.publish(new Message("m" + row.path("id").asLong(), row.path("room_id").asString(), row.path("author").asString(),
        row.path("body").asString(), Instant.parse(row.path("sent_at").asString()).toString()));
  }

  @PreDestroy
  void stop() throws Exception {
    engine.close();
    runner.shutdown();
    runner.awaitTermination(10, TimeUnit.SECONDS);
  }
}
