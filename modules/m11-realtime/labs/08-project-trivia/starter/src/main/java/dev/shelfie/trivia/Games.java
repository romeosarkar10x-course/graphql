package dev.shelfie.trivia;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The game engine: lobby, questions on a timer, scoring, events. */
@Service
class Games {
  private final Map<String, Game> games = new ConcurrentHashMap<>();
  private final AtomicInteger ids = new AtomicInteger();
  private final ScheduledExecutorService clock = Executors.newSingleThreadScheduledExecutor();
  private final List<Game.Question> bank = new ArrayList<>();

  Games(JsonMapper json) throws IOException {
    JsonNode all = json.readTree(new ClassPathResource("questions.json").getInputStream());
    for (JsonNode q : all) {
      List<String> choices = new ArrayList<>();
      q.path("choices").forEach(c -> choices.add(c.asString()));
      bank.add(new Game.Question(bank.size(), q.path("text").asString(), List.copyOf(choices), q.path("correct").asInt()));
    }
  }

  Game get(String id) { return games.get(id); }

  Game require(String id) {
    Game g = games.get(id);
    if (g == null) throw new GameError(ErrorType.NOT_FOUND, "NO_GAME", "no game " + id);
    return g;
  }

  Game create(int questions, int seconds) {
    if (questions < 1 || questions > bank.size()) throw new GameError(ErrorType.BAD_REQUEST, "BAD_ARGUMENT", "questions must be 1 to " + bank.size());
    if (seconds < 1) throw new GameError(ErrorType.BAD_REQUEST, "BAD_ARGUMENT", "secondsPerQuestion must be at least 1");
    Game g = new Game("g" + ids.incrementAndGet(), bank.subList(0, questions), seconds);
    games.put(g.id, g);
    return g;
  }

  Game.Player join(String id, String name) {
    Game g = require(id);
    synchronized (g) {
      if (g.state != Game.State.LOBBY) throw new GameError(ErrorType.BAD_REQUEST, "ALREADY_STARTED", "game " + id + " has started");
      if (g.scores.containsKey(name)) throw new GameError(ErrorType.BAD_REQUEST, "NAME_TAKEN", name + " is taken");
      g.scores.put(name, 0);
      return new Game.Player(name, 0);
    }
  }

  // TODO mini-project 11.8: the game itself. See contract/trivia.graphqls for the rules:
  //  - start: needs a player (NO_PLAYERS), asks question 0 (QuestionAsked) and starts its timer
  //  - answer: NO_QUESTION, UNKNOWN_PLAYER, ALREADY_ANSWERED, BAD_CHOICE, or null when accepted;
  //    100 points for a correct answer, 50 more for the first correct one
  //  - a question closes when everyone has answered or its time is up: QuestionClosed, LeaderboardChanged,
  //    then the next QuestionAsked, or GameOver (and the end of every gameEvents stream) after the last
  Game start(String id) {
    throw new GameError(org.springframework.graphql.execution.ErrorType.INTERNAL_ERROR, "TODO", "startGame is not written yet");
  }

  String answer(String id, String name, int choice) {
    return "NO_QUESTION";
  }
}
