package dev.shelfie.live;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Kafka between instances (shelfie.bus=kafka): every event goes to the topic "shelfie.events", keyed by room. */
@Component
@ConditionalOnProperty(name = "shelfie.bus", havingValue = "kafka")
class KafkaRelay implements Relay {
  static final String TOPIC = "shelfie.events";
  private final KafkaTemplate<String, String> kafka;
  private final LocalBus bus;
  private final JsonMapper json;

  KafkaRelay(KafkaTemplate<String, String> kafka, @Lazy LocalBus bus, JsonMapper json) {
    this.kafka = kafka;
    this.bus = bus;
    this.json = json;
  }

  /** Created at startup if missing (KafkaAdmin), so no instance subscribes to a topic that does not exist yet. */
  @Bean
  static NewTopic shelfieEvents() { return TopicBuilder.name(TOPIC).partitions(3).replicas(1).build(); }

  @Override
  public void send(Event e) { kafka.send(TOPIC, e.roomId(), json.writeValueAsString(e)).join(); }

  @KafkaListener(topics = TOPIC, groupId = "shelfie-live")
  void receive(String value) { bus.deliver(json.readValue(value, Event.class)); }
}
