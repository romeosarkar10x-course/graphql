# Hints: m11/08

1. Keep one `Sinks.Many` per game for its events and one lock per game for its state: answers, the timer
   and the close all touch the same fields from different threads. Returning the sink's `asFlux()` from
   `gameEvents` gives each subscriber the events from now on.
2. A `ScheduledExecutorService` closes a question after `secondsPerQuestion`; remember which question the
   timer was for, so a timer that fires after an early close does nothing. Closing emits `QuestionClosed`,
   then `LeaderboardChanged`, then asks the next question or emits `GameOver`.
3. After `GameOver`, `emitComplete` on the sink ends every subscription. The union needs no type resolver
   when the Java records are named like the GraphQL types (Spring's default `ClassNameTypeResolver`); a
   stable sort of the players by score keeps ties in join order.
