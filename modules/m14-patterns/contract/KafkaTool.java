// Talks to the course Kafka (kafka:9092) from a test, outside any app. Class path: kafka-clients and slf4j-api from
// .cache/m2 (kafka_cp in kafka.sh).
//   KafkaTool send <topic> <key> <value>       one record
//   KafkaTool dump <topic> [seconds]           every record from the beginning as "key<TAB>value", then exit
import java.time.Duration;
import java.util.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.*;

public class KafkaTool {
  public static void main(String[] a) throws Exception {
    Properties p = new Properties();
    p.put("bootstrap.servers", System.getenv().getOrDefault("KAFKA", "kafka:9092"));
    if (a[0].equals("send")) {
      try (var producer = new KafkaProducer<>(p, new StringSerializer(), new StringSerializer())) {
        producer.send(new ProducerRecord<>(a[1], a[2], a[3])).get();
      }
    } else if (a[0].equals("dump")) {
      p.put("group.id", "kafkatool-" + UUID.randomUUID());
      p.put("auto.offset.reset", "earliest");
      p.put("enable.auto.commit", "false");
      long until = System.currentTimeMillis() + 1000L * (a.length > 2 ? Integer.parseInt(a[2]) : 5);
      try (var consumer = new KafkaConsumer<>(p, new StringDeserializer(), new StringDeserializer())) {
        consumer.subscribe(List.of(a[1]));
        while (System.currentTimeMillis() < until)
          for (var r : consumer.poll(Duration.ofMillis(300))) System.out.println(r.key() + "\t" + r.value());
      }
    }
  }
}
