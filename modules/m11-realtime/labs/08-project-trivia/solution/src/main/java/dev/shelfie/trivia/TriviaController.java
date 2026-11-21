package dev.shelfie.trivia;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Map;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Controller
class TriviaController {
  private final Games games;

  TriviaController(Games games) { this.games = games; }

  @QueryMapping
  Game game(@Argument String id) { return games.get(id); }

  @SchemaMapping(typeName = "Game")
  String state(Game g) { synchronized (g) { return g.state.name(); } }

  @SchemaMapping(typeName = "Game")
  List<Game.Player> players(Game g) { synchronized (g) { return g.players(); } }

  @SchemaMapping(typeName = "Game")
  Game.Question question(Game g) { synchronized (g) { return g.question(); } }

  @SchemaMapping(typeName = "Game")
  List<Game.Player> leaderboard(Game g) { synchronized (g) { return g.leaderboard(); } }

  @MutationMapping
  Game createGame(@Argument int questions, @Argument int secondsPerQuestion) { return games.create(questions, secondsPerQuestion); }

  @MutationMapping
  Game.Player joinGame(@Argument String gameId, @Argument String name) { return games.join(gameId, name); }

  @MutationMapping
  Game startGame(@Argument String gameId) { return games.start(gameId); }

  record AnswerResult(boolean accepted, String reason) {}

  @MutationMapping
  AnswerResult answer(@Argument String gameId, @Argument String name, @Argument int choice) {
    String reason = games.answer(gameId, name, choice);
    return new AnswerResult(reason == null, reason);
  }

  // The GameEvent union needs no type resolver: Spring's default, ClassNameTypeResolver, matches the
  // simple class name (Game.QuestionAsked -> QuestionAsked).
  @SubscriptionMapping
  Flux<Object> gameEvents(@Argument String gameId) {
    Game g = games.require(gameId);
    synchronized (g) {
      if (g.state == Game.State.OVER) return Flux.just(new Game.GameOver(g.leaderboard()));
      return g.events.asFlux();
    }
  }

  @Component
  static class Errors extends DataFetcherExceptionResolverAdapter {
    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
      return ex instanceof GameError e
          ? GraphqlErrorBuilder.newError(env).errorType(e.type).message(e.getMessage()).extensions(Map.of("code", e.code)).build()
          : null;
    }
  }
}
