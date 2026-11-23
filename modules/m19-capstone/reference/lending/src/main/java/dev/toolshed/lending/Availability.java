package dev.toolshed.lending;

import dev.toolshed.lending.Model.AvailabilityChange;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Borrow and return events for subscriptions. In memory: one lending instance only. Running two would need
 * the events on a shared channel (PostgreSQL LISTEN/NOTIFY or Kafka, Module 11.4); docs/adr/0006 says why
 * not yet.
 */
@Component
class Availability {
    private final Sinks.Many<AvailabilityChange> sink = Sinks.many().multicast().directBestEffort();

    void publish(AvailabilityChange change) {
        sink.tryEmitNext(change);
    }

    Flux<AvailabilityChange> changes(String toolId) {
        return sink.asFlux().filter(c -> c.tool().id().equals(toolId));
    }
}
