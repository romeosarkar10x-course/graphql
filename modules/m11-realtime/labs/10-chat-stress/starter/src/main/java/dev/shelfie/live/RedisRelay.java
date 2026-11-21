package dev.shelfie.live;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import tools.jackson.databind.json.JsonMapper;

/**
 * Redis pub/sub: PUBLISH to one channel, every instance SUBSCRIBEs. At most once: an instance that is
 * not connected at that moment never sees the event, and Redis keeps nothing.
 */
@Component
@ConditionalOnProperty(name = "shelfie.bus", havingValue = "redis")
class RedisRelay implements Relay {
  static final ChannelTopic CHANNEL = new ChannelTopic("shelfie:events");
  private final ReactiveStringRedisTemplate redis;
  private final ReactiveRedisMessageListenerContainer container;
  private final LocalBus bus;
  private final JsonMapper json;
  private Disposable listening;

  RedisRelay(ReactiveStringRedisTemplate redis, ReactiveRedisConnectionFactory factory, @Lazy LocalBus bus, JsonMapper json) {
    this.redis = redis;
    this.container = new ReactiveRedisMessageListenerContainer(factory);
    this.bus = bus;
    this.json = json;
  }

  @PostConstruct
  void listen() {
    // receiveLater: subscribed when the Mono completes, so nothing published after startup is missed.
    listening = container.receiveLater(CHANNEL).block()
        .subscribe(m -> bus.deliver(json.readValue(m.getMessage(), Event.class)));
  }

  @Override
  public void send(Event e) { redis.convertAndSend(CHANNEL.getTopic(), json.writeValueAsString(e)).block(); }

  @PreDestroy
  void stop() {
    listening.dispose();
    container.destroy();
  }
}
