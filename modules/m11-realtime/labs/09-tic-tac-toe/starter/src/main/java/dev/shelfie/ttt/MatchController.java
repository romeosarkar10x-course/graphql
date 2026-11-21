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

  // TODO game 11.9: refuse illegal moves (see the codes in contract/tictactoe.graphqls) and detect the end.
  @MutationMapping
  Match.View move(@Argument String id, @Argument String name, @Argument int cell) {
    Match m = require(id);
    synchronized (m) {
      String mark = m.players.indexOf(name) == 0 ? "X" : "O";
      m.board[cell] = mark;
      m.moves++;
      m.publish();
      return m.view();
    }
  }

  // TODO game 11.9: a new subscriber should see the board at once, spectators should be counted, and the
  // subscription should end with the match.
  @SubscriptionMapping
  Flux<Match.View> matchUpdated(@Argument String id, @Argument String as) {
    return require(id).updates.asFlux();
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
