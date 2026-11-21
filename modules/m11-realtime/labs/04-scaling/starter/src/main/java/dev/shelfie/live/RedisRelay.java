package dev.shelfie.live;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Redis pub/sub between instances (shelfie.bus=redis). */
@Component
@ConditionalOnProperty(name = "shelfie.bus", havingValue = "redis")
class RedisRelay implements Relay {
  private final ReactiveStringRedisTemplate redis;
  private final ReactiveRedisConnectionFactory factory;
  private final LocalBus bus;
  private final JsonMapper json;

  RedisRelay(ReactiveStringRedisTemplate redis, ReactiveRedisConnectionFactory factory, @Lazy LocalBus bus, JsonMapper json) {
    this.redis = redis;
    this.factory = factory;
    this.bus = bus;
    this.json = json;
  }

  // TODO lab 11.4: publish every event to a Redis channel, and hand every event received on that channel
  // (from any instance, this one included) to bus.deliver(...).
  @Override
  public void send(Event e) {
    bus.deliver(e);
  }
}
