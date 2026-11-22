package dev.shelfie.ops;

import dev.shelfie.ops.ReviewsClient.Review;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

/**
 * Subscription.reviewAdded: one background poller follows the reviews service's /events and fans every new review
 * out to all subscribers of this replica. Two replicas each poll, so either can serve any subscriber.
 *
 * Module 16.4: a client that reconnects passes the last seq it saw as `after`; it first gets every review it
 * missed (from the reviews service), then the live feed, without gaps or duplicates. On shutdown (Shutdown.java)
 * every subscription completes, so clients reconnect to another replica at once instead of waiting for a dead
 * socket to time out.
 */
@Controller
public class LiveController {
    private final ReviewsClient reviews;
    private final Sinks.Many<Review> sink = Sinks.many().multicast().directBestEffort();
    private final Sinks.Empty<Void> closing = Sinks.empty();
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
                    if (!first) sink.emitNext(r, Sinks.EmitFailureHandler.busyLooping(Duration.ofMillis(100)));
                }
                first = false;
            } catch (Exception e) {
                try { Thread.sleep(500); } catch (InterruptedException ie) { return; }
            }
        }
    }

    @SubscriptionMapping
    public Flux<Review> reviewAdded(@Argument Integer after) {
        Flux<Review> feed;
        if (after == null) {
            feed = sink.asFlux();
        } else {
            // Listen first (into a per-subscription buffer), then fetch what was missed, then drain the buffer:
            // nothing falls between the two, and the seq filter drops what both delivered.
            feed = Flux.defer(() -> {
                Sinks.Many<Review> buffer = Sinks.many().unicast().onBackpressureBuffer();
                Disposable listening = sink.asFlux().subscribe(buffer::tryEmitNext);
                Flux<Review> missed = Mono.fromCallable(() -> reviews.events(after, 0))
                        .subscribeOn(Schedulers.boundedElastic()).flatMapIterable(list -> list);
                AtomicInteger seen = new AtomicInteger(after);
                return Flux.concat(missed, buffer.asFlux()).filter(r -> r.seq() > seen.get())
                        .doOnNext(r -> seen.set(r.seq())).doFinally(signal -> listening.dispose());
            });
        }
        return feed.takeUntilOther(closing.asMono());
    }

    /** Complete every open subscription (Shutdown calls it once the replica is out of the balancer). */
    public void completeAll() {
        running.set(false);
        closing.tryEmitEmpty();
    }
}
