package dev.shelfie.ttt;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import reactor.core.publisher.Sinks;

/** One match: its state, guarded by its own lock, and a sink of snapshots for its subscribers. */
class Match {
  static final int[][] LINES = {{0, 1, 2}, {3, 4, 5}, {6, 7, 8}, {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, {0, 4, 8}, {2, 4, 6}};

  /** What subscribers and queries see: an immutable copy, so a later move cannot change a sent event. */
  record View(String id, List<String> board, String next, String winner, List<Integer> line, boolean draw,
              List<String> players, int spectators, int moves) {}

  final String id;
  final String[] board = {"", "", "", "", "", "", "", "", ""};
  final List<String> players = new ArrayList<>();
  final AtomicInteger spectators = new AtomicInteger();
  int moves;
  String winner;
  int[] line;
  final Sinks.Many<View> updates = Sinks.many().multicast().directBestEffort();

  Match(String id) { this.id = id; }

  boolean over() { return winner != null || moves == 9; }

  String next() {
    if (players.size() < 2 || over()) return null;
    return moves % 2 == 0 ? "X" : "O";
  }

  View view() {
    return new View(id, List.of(board), next(), winner, line == null ? null : Arrays.stream(line).boxed().toList(),
        winner == null && moves == 9, List.copyOf(players), spectators.get(), moves);
  }

  void publish() {
    updates.emitNext(view(), Sinks.EmitFailureHandler.busyLooping(Duration.ofMillis(50)));
    if (over()) updates.emitComplete(Sinks.EmitFailureHandler.busyLooping(Duration.ofMillis(50)));
  }

  /** After a move: three in a row for the mark just played? */
  void judge(String mark) {
    for (int[] l : LINES)
      if (board[l[0]].equals(mark) && board[l[1]].equals(mark) && board[l[2]].equals(mark)) {
        winner = mark;
        line = l;
        return;
      }
  }
}
