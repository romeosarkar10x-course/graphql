package dev.shelfie.ttt;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

@Controller
class MatchController {
  private final Map<String, Match> matches = new ConcurrentHashMap<>();
  private final AtomicInteger ids = new AtomicInteger();

  static class Refused extends RuntimeException {
    final String code;
    Refused(String code, String message) { super(message); this.code = code; }
  }

  private Match require(String id) {
    Match m = matches.get(id);
    if (m == null) throw new Refused("NO_MATCH", "no match " + id);
    return m;
  }

  @QueryMapping
  Match.View match(@Argument String id) {
    Match m = matches.get(id);
    if (m == null) return null;
    synchronized (m) { return m.view(); }
  }

  @MutationMapping
  Match.View newMatch() {
    Match m = new Match("t" + ids.incrementAndGet());
    matches.put(m.id, m);
    return m.view();
  }

  @MutationMapping
  String joinMatch(@Argument String id, @Argument String name) {
    Match m = require(id);
    synchronized (m) {
      if (m.players.contains(name)) return m.players.indexOf(name) == 0 ? "X" : "O";   // joining twice is harmless
      if (m.players.size() == 2) throw new Refused("MATCH_FULL", "match " + id + " already has two players");
      m.players.add(name);
      m.publish();
      return m.players.size() == 1 ? "X" : "O";
    }
  }

  @MutationMapping
  Match.View move(@Argument String id, @Argument String name, @Argument int cell) {
    Match m = require(id);
    synchronized (m) {
      if (!m.players.contains(name)) throw new Refused("NOT_A_PLAYER", name + " is not playing in " + id);
      if (m.over()) throw new Refused("GAME_OVER", "the match is over");
      if (m.players.size() < 2) throw new Refused("WAITING_FOR_OPPONENT", "nobody has joined as O yet");
      String mark = m.players.indexOf(name) == 0 ? "X" : "O";
      if (!mark.equals(m.next())) throw new Refused("NOT_YOUR_TURN", "it is " + m.next() + "'s move");
      if (cell < 0 || cell > 8) throw new Refused("BAD_CELL", "cells are 0 to 8");
      if (!m.board[cell].isEmpty()) throw new Refused("CELL_TAKEN", "cell " + cell + " is taken");
      m.board[cell] = mark;
      m.moves++;
      m.judge(mark);
      m.publish();
      return m.view();
    }
  }

  /** The current state first, so a spectator who joins mid-game sees the board at once; then every change. */
  @SubscriptionMapping
  Flux<Match.View> matchUpdated(@Argument String id, @Argument String as) {
    Match m = require(id);
    synchronized (m) {
      Match.View now = m.view();
      if (m.over()) return Flux.just(now);
      boolean spectator = as == null || !m.players.contains(as);
      Flux<Match.View> later = m.updates.asFlux();
      if (!spectator) return later.startWith(now);
      return later.startWith(now)
          .doOnSubscribe(s -> { m.spectators.incrementAndGet(); synchronized (m) { m.publish(); } })
          .doFinally(s -> m.spectators.decrementAndGet());
    }
  }

  @Component
  static class Errors extends DataFetcherExceptionResolverAdapter {
    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
      return ex instanceof Refused r
          ? GraphqlErrorBuilder.newError(env).errorType(r.code.equals("NO_MATCH") ? ErrorType.NOT_FOUND : ErrorType.BAD_REQUEST)
              .message(r.getMessage()).extensions(Map.of("code", r.code)).build()
          : null;
    }
  }
}
