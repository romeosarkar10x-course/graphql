package dev.shelfie.trivia;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import reactor.core.publisher.Sinks;

/** One game. Every method that changes it holds its lock; events go out through its own sink. */
class Game {
  enum State { LOBBY, QUESTION, OVER }

  record Question(int index, String text, List<String> choices, int correct) {}
  record Player(String name, int score) {}

  // Events, one record per union member.
  record QuestionAsked(Question question, int secondsToAnswer) {}
  record QuestionClosed(int index, int correct, int answers) {}
  record LeaderboardChanged(List<Player> leaderboard) {}
  record GameOver(List<Player> leaderboard) {}

  final String id;
  final List<Question> questions;
  final int seconds;
  State state = State.LOBBY;
  int current = -1;
  final LinkedHashMap<String, Integer> scores = new LinkedHashMap<>();
  final Map<String, Integer> answers = new HashMap<>();   // this question: name -> choice
  boolean firstCorrectTaken;
  ScheduledFuture<?> timer;
  // replay(…) would resend history to late subscribers; a quiz wants "from now on".
  final Sinks.Many<Object> events = Sinks.many().multicast().directBestEffort();

  Game(String id, List<Question> questions, int seconds) {
    this.id = id;
    this.questions = questions;
    this.seconds = seconds;
  }

  Question question() { return state == State.QUESTION ? questions.get(current) : null; }

  List<Player> leaderboard() {
    List<Player> all = new ArrayList<>(players());
    all.sort(Comparator.comparingInt(Player::score).reversed()); // stable: ties stay in join order
    return all;
  }

  List<Player> players() { return scores.entrySet().stream().map(e -> new Player(e.getKey(), e.getValue())).toList(); }

  void emit(Object event) { events.emitNext(event, Sinks.EmitFailureHandler.busyLooping(Duration.ofMillis(50))); }
}
