package dev.shelfie.ops;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Module 16.4: a rolling deploy without dropped messages. On SIGTERM, before the web server's own graceful
 * shutdown (this lifecycle's phase is higher, so it stops first):
 *
 *   1. readiness becomes REFUSING_TRAFFIC: /actuator/health/readiness answers 503 and the balancer stops sending
 *      new connections here;
 *   2. wait shelfie.shutdown.drain (default 2 s, longer than the balancer takes to notice; Kubernetes: a preStop
 *      sleep). Meanwhile every response says "Connection: close", so clients stop reusing kept-alive connections
 *      to this replica and open new ones, which the balancer sends elsewhere;
 *   3. complete every subscription: clients reconnect elsewhere and resume with `after`;
 *   4. then server.shutdown=graceful lets in-flight HTTP requests finish.
 */
@Component
public class Shutdown implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(Shutdown.class);
    private final ApplicationEventPublisher events;
    private final LiveController live;
    private final long drainMs;
    private volatile boolean running;
    private volatile boolean draining;

    public Shutdown(ApplicationEventPublisher events, LiveController live,
                    @Value("${shelfie.shutdown.drain:2s}") java.time.Duration drain) {
        this.events = events;
        this.live = live;
        this.drainMs = drain.toMillis();
    }

    @Override public void start() { running = true; }
    @Override public boolean isRunning() { return running; }
    @Override public int getPhase() { return Integer.MAX_VALUE; }

    @Override
    public void stop() {
        draining = true;
        AvailabilityChangeEvent.publish(events, this, ReadinessState.REFUSING_TRAFFIC);
        log.info("readiness REFUSING_TRAFFIC; draining for {} ms", drainMs);
        try { Thread.sleep(drainMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        live.completeAll();
        log.info("completed every subscription");
        running = false;
    }

    /** While draining, ask every client to close its connection after this response. */
    @Bean
    FilterRegistrationBean<OncePerRequestFilter> connectionClose() {
        return new FilterRegistrationBean<>(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                if (draining) response.setHeader("Connection", "close");
                chain.doFilter(request, response);
            }
        });
    }
}
