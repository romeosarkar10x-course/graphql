package dev.shelfie.ops;

import dev.shelfie.ops.ReviewsClient.Review;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Subscription.reviewAdded: one background poller follows the reviews service's /events and fans every new review
 * out to all subscribers of this replica. Two replicas each poll, so either can serve any subscriber.
 */
@Controller
public class LiveController {
    private final ReviewsClient reviews;
    private final Sinks.Many<Review> sink = Sinks.many().multicast().directBestEffort();
    private final AtomicInteger lastSeq = new AtomicInteger();
    private final AtomicBoolean running = new AtomicBoolean(true);

    public LiveController(ReviewsClient reviews) {
        this.reviews = reviews;
        boolean primed = prime();
        Thread.ofVirtual().name("reviews-poller").start(() -> poll(!primed));
    }

    /** Start from the newest review before the server accepts subscribers (a later start would skip some). */
    private boolean prime() {
        try {
            var existing = reviews.events(0, 0);
            if (!existing.isEmpty()) lastSeq.set(existing.getLast().seq());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void poll(boolean first) {
        while (running.get()) {
            try {
                var batch = reviews.events(lastSeq.get(), first ? 0 : 5000);
                for (Review r : batch) {
                    lastSeq.set(r.seq());
                    if (!first) sink.emitNext(r, Sinks.EmitFailureHandler.busyLooping(java.time.Duration.ofMillis(100)));
                }
                first = false;
            } catch (Exception e) {
                try { Thread.sleep(500); } catch (InterruptedException ie) { return; }
            }
        }
    }

    @SubscriptionMapping
    public Flux<Review> reviewAdded(@Argument Integer after) {
        return sink.asFlux();
    }

    @PreDestroy
    void stop() { running.set(false); }
}
