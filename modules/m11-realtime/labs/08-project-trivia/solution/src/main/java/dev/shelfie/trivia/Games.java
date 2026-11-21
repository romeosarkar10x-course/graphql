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

  Game start(String id) {
    Game g = require(id);
    synchronized (g) {
      if (g.state != Game.State.LOBBY) throw new GameError(ErrorType.BAD_REQUEST, "ALREADY_STARTED", "game " + id + " has started");
      if (g.scores.isEmpty()) throw new GameError(ErrorType.BAD_REQUEST, "NO_PLAYERS", "nobody has joined");
      ask(g, 0);
      return g;
    }
  }

  String answer(String id, String name, int choice) {
    Game g = require(id);
    synchronized (g) {
      if (g.state != Game.State.QUESTION) return "NO_QUESTION";
      if (!g.scores.containsKey(name)) return "UNKNOWN_PLAYER";
      if (g.answers.containsKey(name)) return "ALREADY_ANSWERED";
      Game.Question q = g.question();
      if (choice < 0 || choice >= q.choices().size()) return "BAD_CHOICE";
      g.answers.put(name, choice);
      if (choice == q.correct()) {
        int points = 100 + (g.firstCorrectTaken ? 0 : 50);
        g.firstCorrectTaken = true;
        g.scores.merge(name, points, Integer::sum);
      }
      if (g.answers.size() == g.scores.size()) close(g);   // everyone answered: do not make them wait
      return null;
    }
  }

  private void ask(Game g, int index) {
    g.state = Game.State.QUESTION;
    g.current = index;
    g.answers.clear();
    g.firstCorrectTaken = false;
    g.emit(new Game.QuestionAsked(g.question(), g.seconds));
    int asked = index;
    g.timer = clock.schedule(() -> { synchronized (g) { if (g.state == Game.State.QUESTION && g.current == asked) close(g); } },
        g.seconds, TimeUnit.SECONDS);
  }

  /** Called with the game's lock held. */
  private void close(Game g) {
    if (g.timer != null) g.timer.cancel(false);
    Game.Question q = g.question();
    g.emit(new Game.QuestionClosed(q.index(), q.correct(), g.answers.size()));
    g.emit(new Game.LeaderboardChanged(g.leaderboard()));
    if (g.current + 1 < g.questions.size()) ask(g, g.current + 1);
    else {
      g.state = Game.State.OVER;
      g.emit(new Game.GameOver(g.leaderboard()));
      g.events.emitComplete(Sinks.EmitFailureHandler.busyLooping(java.time.Duration.ofMillis(50)));
    }
  }
}
